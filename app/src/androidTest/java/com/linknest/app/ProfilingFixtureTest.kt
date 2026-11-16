package com.linknest.app

import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.testing.ProfilingEntryPoint
import com.linknest.core.data.model.CategoryUpsertRequest
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicitly invoked setup/cleanup for emulator traces; never replaces the existing library. */
class ProfilingFixtureTest {
    @Test fun configure() = runBlocking {
        val action = InstrumentationRegistry.getArguments().getString("fixtureAction")
        assumeTrue(action == "create" || action == "cleanup")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entry = EntryPointAccessors.fromApplication(context, ProfilingEntryPoint::class.java)
        val marker = File(context.cacheDir, "linknest-profiling-fixture.json")
        if (marker.exists()) {
            val previous = JSONObject(marker.readText())
            val id = previous.getLong("id")
            val category = entry.categories().getCategoryById(id)
            if (category != null) {
                check(category.name == previous.getString("name")) { "Fixture category identity changed; preserve it." }
                entry.categories().deleteCategory(id)
            }
            check(marker.delete()) { "Unable to remove the old fixture marker." }
        }
        if (action == "create") {
            val name = "LinkNest QA profiling ${System.nanoTime()}"
            val category = entry.categories().upsertCategory(CategoryUpsertRequest(
                name = name, colorHex = "#7C4DFF", iconType = IconType.EMOJI, iconValue = "*",
            ))
            marker.writeText(JSONObject().put("id", category.id).put("name", name).toString())
            val now = System.currentTimeMillis()
            repeat(300) { index ->
                entry.websites().addWebsite(PersistWebsiteRequest(
                    categoryId = category.id, title = "QA link ${index + 1}",
                    canonicalUrl = null, finalUrl = null, normalizedUrl = "https://profiling.invalid/${index + 1}",
                    domain = "profiling.invalid", ogImageUrl = null, faviconUrl = null,
                    chosenIconSource = IconSource.GENERATED, customIconUri = null, emojiIcon = null,
                    tileSizeDp = null, sortOrder = index, isPinned = index == 0,
                    healthStatus = HealthStatus.UNKNOWN, createdAt = now, updatedAt = now, lastCheckedAt = now,
                ))
            }
        }
    }
}
