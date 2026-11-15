package com.linknest.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MigrationPreservationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), LinkNestDatabase::class.java,
    )

    @Test fun migrateV2_preservesWebsiteTagAssociation() {
        val name = "migration-preservation-${System.nanoTime()}"
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO categories VALUES (1, 'Inbox', '#000000', 'AUTO', NULL, 0, 0, 0, 1, 1)")
            execSQL("INSERT INTO tags VALUES (1, 'Saved')")
            execSQL("""INSERT INTO website_entries
                (id, category_id, title, normalized_url, domain, chosen_icon_source, sort_order,
                 is_pinned, open_count, health_status, created_at, updated_at)
                VALUES (1, 1, 'Example', 'https://example.com/', 'example.com', 'GENERATED', 0, 0, 0, 'UNKNOWN', 1, 1)""")
            execSQL("INSERT INTO website_tag_cross_ref VALUES (1, 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, *DatabaseMigrations.ALL).use { database ->
            database.query("SELECT COUNT(*) FROM website_tag_cross_ref WHERE website_id=1 AND tag_id=1").use {
                it.moveToFirst(); assertEquals(1, it.getInt(0))
            }
        }
    }
}
