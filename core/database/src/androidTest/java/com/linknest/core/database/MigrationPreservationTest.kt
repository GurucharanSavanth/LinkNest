package com.linknest.core.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MigrationPreservationTest(private val version: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "schema {0} to 6")
        fun versions(): List<Array<Int>> = (1..6).map { arrayOf(it) }
    }

    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), LinkNestDatabase::class.java,
    )

    @Test fun upgrade_preservesLibraryAndAssociations() {
        val name = "migration-preservation-$version-${System.nanoTime()}"
        helper.createDatabase(name, version).apply {
            execSQL("INSERT INTO categories VALUES (1, 'Inbox', '#112233', 'AUTO', NULL, 0, 0, 0, 1, 1)")
            execSQL("INSERT INTO tags VALUES (1, 'Saved')")
            insert("website_entries", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", 1L); put("category_id", 1L); put("title", "Example")
                put("normalized_url", "https://example.com/"); put("domain", "example.com")
                put("canonical_url", "https://example.com/canonical")
                put("chosen_icon_source", "GENERATED"); put("is_pinned", 1)
                put("open_count", 7); put("last_opened_at", 10L); put("last_checked_at", 11L)
                put("health_status", if (version == 1) "HEALTHY" else "OK")
                put("created_at", 1L); put("updated_at", 2L)
                if (version >= 2) put("sort_order", 4)
                if (version >= 3) {
                    put("final_url", "https://example.com/final")
                    put("note", "Personal note"); put("priority", "NORMAL"); put("follow_up_status", "NONE")
                }
            })
            execSQL("INSERT INTO website_tag_cross_ref VALUES (1, 1)")
            insert("icon_cache", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", 1L); put("website_id", 1L); put("source_url", "https://example.com/icon.png")
                put("fetched_at", 1L); put("updated_at", 2L)
            })
            if (version >= 2) execSQL("INSERT INTO domain_category_mapping VALUES ('example.com', 1, 3, 2)")
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, *DatabaseMigrations.ALL).use { database ->
            database.query("SELECT COUNT(*) FROM website_tag_cross_ref WHERE website_id=1 AND tag_id=1").use {
                it.moveToFirst(); assertEquals(1, it.getInt(0))
            }
            database.query("SELECT title, open_count, is_pinned, health_status, sort_order, note FROM website_entries WHERE id=1").use {
                it.moveToFirst()
                assertEquals("Example", it.getString(0)); assertEquals(7, it.getInt(1))
                assertEquals(1, it.getInt(2)); assertEquals("OK", it.getString(3))
                assertEquals(if (version == 1) 1 else 4, it.getInt(4))
                assertEquals(if (version >= 3) "Personal note" else null, it.getString(5))
            }
            database.query("SELECT COUNT(*) FROM icon_cache WHERE website_id=1").use {
                it.moveToFirst(); assertEquals(1, it.getInt(0))
            }
            if (version >= 2) database.query("SELECT usage_count FROM domain_category_mapping WHERE category_id=1").use {
                it.moveToFirst(); assertEquals(3, it.getInt(0))
            }
        }
    }
}
