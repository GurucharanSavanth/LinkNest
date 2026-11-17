package com.linknest.core.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.core.common.time.TimeProvider
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.network.UrlMetadataFetcher
import com.linknest.core.network.model.MetadataResult
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class MetadataRefreshPreservationTest {
    @Test fun generatedFallback_preservesAllExistingMetadataAndPersonalFields() = runBlocking {
        fixture(IconSource.EMOJI) { f ->
            val before = f.entry()
            f.repository.refreshStaleMetadata(10, 20) { url ->
                MetadataResult("example.com", null, url.normalizedUrl, "example.com", null, null, IconSource.GENERATED)
            }
            assertEquals(before.copy(lastCheckedAt = 1_000, healthStatus = HealthStatus.UNKNOWN, updatedAt = 1_000), f.entry())
            assertEquals(1, f.indexWrites.get())
        }
    }

    @Test fun successfulRefresh_preservesUserTitleAndExplicitIconChoices() = runBlocking {
        for (source in listOf(IconSource.EMOJI, IconSource.CUSTOM, IconSource.REL_ICON)) {
            fixture(source) { f ->
                val before = f.entry()
                f.repository.refreshStaleMetadata(10, 20) { freshMetadata() }
                val expectedSource = if (source == IconSource.REL_ICON) IconSource.OG_IMAGE else source
                assertEquals(before.copy(canonicalUrl = "https://example.com/new-canonical",
                    finalUrl = "https://example.com/new-final", ogImageUrl = "https://example.com/new-image",
                    faviconUrl = "https://example.com/new-icon", chosenIconSource = expectedSource,
                    lastCheckedAt = 1_000, healthStatus = HealthStatus.REDIRECTED, updatedAt = 1_000), f.entry())
                assertEquals(1, f.indexWrites.get())
            }
        }
    }

    @Test fun concurrentEditOrReplacement_isNotOverwrittenByOldFetch() = runBlocking {
        for (replaceUrl in listOf(false, true)) {
            fixture(IconSource.EMOJI) { f ->
                val before = f.entry()
                val revised = if (replaceUrl) before.copy(normalizedUrl = "https://other.example/new", title = "Restored")
                    else before.copy(title = "Edited while fetching", note = "New note", updatedAt = 2)
                f.repository.refreshStaleMetadata(10, 20) {
                    f.database.websiteDao().updateWebsite(revised)
                    freshMetadata()
                }
                assertEquals(revised, f.entry())
                assertEquals("An unchanged row must not be reindexed as an updated row", 0, f.indexWrites.get())
            }
        }
    }

    private fun freshMetadata() = MetadataResult("Remote page title", "https://example.com/new-canonical",
        "https://example.com/new-final", "example.com", "https://example.com/new-image", "https://example.com/new-icon", IconSource.OG_IMAGE)

    private suspend fun fixture(source: IconSource, block: suspend (Fixture) -> Unit) {
        val fixture = Fixture()
        try { fixture.seed(source); block(fixture) } finally { fixture.database.close() }
    }

    private class Fixture {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LinkNestDatabase::class.java).build()
        val indexWrites = AtomicInteger()
        private val index = Proxy.newProxyInstance(SearchIndexRepository::class.java.classLoader,
            arrayOf(SearchIndexRepository::class.java)) { _, method, _ ->
            check(method.name == "indexWebsite") { "Unexpected index operation: ${method.name}" }
            indexWrites.incrementAndGet()
            Unit
        } as SearchIndexRepository
        val repository = OfflineFirstWebsiteRepository(database, database.websiteDao(), database.iconCacheDao(),
            database.tagDao(), UrlMetadataFetcher(Dispatchers.IO), object : TimeProvider {
                override fun now() = 1_000L
            }, index, Dispatchers.Default)

        suspend fun entry(): WebsiteEntryEntity = database.websiteDao().getWebsiteById(1)!!

        suspend fun seed(source: IconSource) = withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL("INSERT INTO categories VALUES (1, 'Inbox', '#112233', 'AUTO', NULL, 0, 0, 0, 1, 1)")
            database.openHelper.writableDatabase.execSQL("""INSERT INTO website_entries
                (id, category_id, title, normalized_url, domain, canonical_url, final_url, og_image_url,
                 favicon_url, chosen_icon_source, custom_icon_uri, emoji_icon, sort_order, is_pinned, open_count,
                 health_status, note, priority, follow_up_status, created_at, updated_at)
                VALUES (1, 1, 'My title', 'https://example.com/', 'example.com', 'https://example.com/canonical',
                 'https://example.com/final', 'https://example.com/image', 'https://example.com/icon', ?,
                 'file:///owned-custom.png', 'A', 7, 1, 5, 'OK', 'Keep note', 'HIGH', 'NONE', 1, 1)""", arrayOf<Any>(source.name))
        }
    }
}
