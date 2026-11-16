package com.linknest.core.data.repository

import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.core.common.time.TimeProvider
import com.linknest.core.database.LinkNestDatabase
import com.linknest.core.network.UrlMetadataFetcher
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class LibraryHydrationTest {
    @Test fun bulkHydration_usesFourReadsAtEveryLibrarySize() = runBlocking {
        for (size in listOf(100, 1_000, 5_000)) {
            val reads = AtomicInteger()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val database = Room.inMemoryDatabaseBuilder(context, LinkNestDatabase::class.java)
                .setQueryCallback(object : RoomDatabase.QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        if (sqlQuery.trimStart().startsWith("SELECT", ignoreCase = true)) reads.incrementAndGet()
                    }
                }, Executor { it.run() }).build()
            try {
                withContext(Dispatchers.IO) {
                    val sql = database.openHelper.writableDatabase
                    sql.beginTransaction()
                    try {
                        sql.execSQL("INSERT INTO categories VALUES (1, 'Inbox', '#112233', 'AUTO', NULL, 0, 0, 0, 1, 1)")
                        sql.compileStatement("""INSERT INTO website_entries
                            (id, category_id, title, normalized_url, domain, chosen_icon_source, sort_order,
                             is_pinned, open_count, health_status, priority, follow_up_status, created_at, updated_at)
                            VALUES (?, 1, ?, ?, 'example.com', 'GENERATED', ?, 0, 0, 'UNKNOWN', 'NORMAL', 'NONE', 1, 1)""").use { statement ->
                            for (id in 1..size) {
                                statement.bindLong(1, id.toLong()); statement.bindString(2, "Link $id")
                                statement.bindString(3, "https://example.com/$id"); statement.bindLong(4, id.toLong())
                                statement.executeInsert()
                            }
                        }
                        sql.execSQL("INSERT INTO tags VALUES (1, 'Saved')")
                        sql.execSQL("INSERT INTO website_tag_cross_ref VALUES (1, 1)")
                        sql.setTransactionSuccessful()
                    } finally { sql.endTransaction() }
                }
                val unusedIndex = Proxy.newProxyInstance(SearchIndexRepository::class.java.classLoader,
                    arrayOf(SearchIndexRepository::class.java)) { _, _, _ -> error("Hydration must not access the index") } as SearchIndexRepository
                val repository = OfflineFirstWebsiteRepository(database, database.websiteDao(), database.iconCacheDao(),
                    database.tagDao(), UrlMetadataFetcher(Dispatchers.IO), object : TimeProvider {
                        override fun now() = 1L
                    }, unusedIndex, Dispatchers.Default)
                reads.set(0)
                val started = System.nanoTime()
                val items = repository.getAllWebsiteItems()
                val elapsed = (System.nanoTime() - started) / 1_000_000
                assertEquals(size, items.size)
                assertEquals(listOf("Saved"), items.first { it.id == 1L }.tagNames)
                assertTrue(items.all { it.duplicateCount == 0 })
                assertEquals("Read count must not grow with library size", 4, reads.get())
                Log.i("LinkNestPerf", "bulkHydration size=$size reads=${reads.get()} elapsedMs=$elapsed")
            } finally { database.close() }
        }
    }
}
