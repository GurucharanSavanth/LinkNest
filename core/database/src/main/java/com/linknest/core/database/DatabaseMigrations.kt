package com.linknest.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE website_entries
                ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
            )
            database.execSQL(
                """
                UPDATE website_entries
                SET sort_order = id
                WHERE sort_order = 0
                """.trimIndent(),
            )
            database.execSQL(
                """
                UPDATE website_entries
                SET health_status = CASE health_status
                    WHEN 'HEALTHY' THEN 'OK'
                    WHEN 'BROKEN' THEN 'DEAD'
                    ELSE health_status
                END
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS domain_category_mapping (
                    domain TEXT NOT NULL,
                    category_id INTEGER NOT NULL,
                    usage_count INTEGER NOT NULL,
                    last_used_at INTEGER NOT NULL,
                    PRIMARY KEY(domain, category_id),
                    FOREIGN KEY(category_id) REFERENCES categories(id)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_domain_category_mapping_domain
                ON domain_category_mapping(domain)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_domain_category_mapping_category_id
                ON domain_category_mapping(category_id)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_domain_category_mapping_usage_count
                ON domain_category_mapping(usage_count)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_domain_category_mapping_last_used_at
                ON domain_category_mapping(last_used_at)
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE icon_cache
                ADD COLUMN content_hash TEXT
                """.trimIndent(),
            )
            database.execSQL(
                """
                ALTER TABLE icon_cache
                ADD COLUMN mime_type TEXT
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_website_entries_sort_order
                ON website_entries(sort_order)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_icon_cache_content_hash
                ON icon_cache(content_hash)
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS website_entries_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    category_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    canonical_url TEXT,
                    final_url TEXT,
                    normalized_url TEXT NOT NULL,
                    domain TEXT NOT NULL,
                    og_image_url TEXT,
                    favicon_url TEXT,
                    chosen_icon_source TEXT NOT NULL,
                    custom_icon_uri TEXT,
                    emoji_icon TEXT,
                    tile_size_dp INTEGER,
                    sort_order INTEGER NOT NULL,
                    is_pinned INTEGER NOT NULL,
                    open_count INTEGER NOT NULL,
                    last_opened_at INTEGER,
                    last_checked_at INTEGER,
                    health_status TEXT NOT NULL,
                    note TEXT,
                    reason_saved TEXT,
                    priority TEXT NOT NULL DEFAULT 'NORMAL',
                    follow_up_status TEXT NOT NULL DEFAULT 'NONE',
                    revisit_at INTEGER,
                    source_label TEXT,
                    custom_label TEXT,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    FOREIGN KEY(category_id) REFERENCES categories(id)
                        ON UPDATE CASCADE
                        ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO website_entries_new (
                    id,
                    category_id,
                    title,
                    canonical_url,
                    final_url,
                    normalized_url,
                    domain,
                    og_image_url,
                    favicon_url,
                    chosen_icon_source,
                    custom_icon_uri,
                    emoji_icon,
                    tile_size_dp,
                    sort_order,
                    is_pinned,
                    open_count,
                    last_opened_at,
                    last_checked_at,
                    health_status,
                    note,
                    reason_saved,
                    priority,
                    follow_up_status,
                    revisit_at,
                    source_label,
                    custom_label,
                    created_at,
                    updated_at
                )
                SELECT
                    id,
                    category_id,
                    title,
                    canonical_url,
                    canonical_url,
                    normalized_url,
                    domain,
                    og_image_url,
                    favicon_url,
                    chosen_icon_source,
                    custom_icon_uri,
                    emoji_icon,
                    tile_size_dp,
                    sort_order,
                    is_pinned,
                    open_count,
                    last_opened_at,
                    last_checked_at,
                    health_status,
                    NULL,
                    NULL,
                    'NORMAL',
                    'NONE',
                    NULL,
                    NULL,
                    NULL,
                    created_at,
                    updated_at
                FROM website_entries
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE website_entries")
            database.execSQL("ALTER TABLE website_entries_new RENAME TO website_entries")

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_category_id ON website_entries(category_id)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_normalized_url ON website_entries(normalized_url)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_final_url ON website_entries(final_url)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_domain ON website_entries(domain)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_is_pinned ON website_entries(is_pinned)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_health_status ON website_entries(health_status)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_priority ON website_entries(priority)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_follow_up_status ON website_entries(follow_up_status)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_revisit_at ON website_entries(revisit_at)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_sort_order ON website_entries(sort_order)",
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_filters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    spec_json TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_saved_filters_name
                ON saved_filters(name)
                """.trimIndent(),
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS integrity_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    successful INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_integrity_events_type
                ON integrity_events(type)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_integrity_events_created_at
                ON integrity_events(created_at)
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_category_id_sort_order ON website_entries(category_id, sort_order)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_domain_title ON website_entries(domain, title)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_health_status_last_checked_at ON website_entries(health_status, last_checked_at)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_last_opened_at ON website_entries(last_opened_at)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_created_at ON website_entries(created_at)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_website_entries_updated_at ON website_entries(updated_at)",
            )
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                UPDATE website_entries
                SET final_url = NULL
                WHERE last_checked_at IS NULL
                  AND canonical_url IS NOT NULL
                  AND final_url = canonical_url
                  AND normalized_url != final_url
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recent_queries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    query TEXT NOT NULL,
                    use_count INTEGER NOT NULL,
                    last_used_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_recent_queries_query
                ON recent_queries(query)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_recent_queries_last_used_at
                ON recent_queries(last_used_at)
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
    )
}
