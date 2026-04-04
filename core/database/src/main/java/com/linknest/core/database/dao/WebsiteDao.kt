package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.linknest.core.database.entity.WebsiteEntryEntity
import com.linknest.core.database.model.DuplicateWebsiteRow
import com.linknest.core.database.model.SearchWebsiteRow
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource

@Dao
interface WebsiteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWebsite(entity: WebsiteEntryEntity): Long

    @Update
    suspend fun updateWebsite(entity: WebsiteEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWebsites(entities: List<WebsiteEntryEntity>): List<Long>

    @Query("SELECT * FROM website_entries WHERE id = :websiteId LIMIT 1")
    suspend fun getWebsiteById(websiteId: Long): WebsiteEntryEntity?

    @Query("SELECT * FROM website_entries ORDER BY created_at ASC")
    suspend fun getAllWebsites(): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries
        WHERE last_opened_at IS NOT NULL
        ORDER BY last_opened_at DESC, updated_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentEntries(limit: Int): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries
        WHERE open_count > 0
        ORDER BY open_count DESC, COALESCE(last_opened_at, 0) DESC, updated_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getMostUsedEntries(limit: Int): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries
        WHERE health_status IN ('BLOCKED', 'LOGIN_REQUIRED', 'REDIRECTED', 'DNS_FAILED', 'SSL_ISSUE', 'DEAD', 'TIMEOUT')
           OR follow_up_status != 'NONE'
        ORDER BY COALESCE(revisit_at, last_checked_at, last_opened_at, updated_at) DESC
        LIMIT :limit
        """,
    )
    suspend fun getNeedsAttentionEntries(limit: Int): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries w
        WHERE w.normalized_url IN (
            SELECT normalized_url
            FROM website_entries
            GROUP BY normalized_url
            HAVING COUNT(*) > 1
        )
           OR (
                w.final_url IS NOT NULL AND w.final_url IN (
                    SELECT final_url
                    FROM website_entries
                    WHERE final_url IS NOT NULL
                    GROUP BY final_url
                    HAVING COUNT(*) > 1
                )
            )
           OR EXISTS (
                SELECT 1
                FROM website_entries w2
                WHERE w2.id != w.id
                  AND w2.domain = w.domain
                  AND LOWER(w2.title) = LOWER(w.title)
            )
        ORDER BY updated_at DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun getPotentialDuplicateEntries(limit: Int): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries w
        WHERE COALESCE(TRIM(w.reason_saved), '') = ''
          AND COALESCE(TRIM(w.note), '') = ''
          AND w.is_pinned = 0
          AND NOT EXISTS (
              SELECT 1 FROM website_tag_cross_ref wt
              WHERE wt.website_id = w.id
          )
        ORDER BY created_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getUnsortedEntries(limit: Int): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun getLastImportedEntries(limit: Int): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries
        WHERE normalized_url = :normalizedUrl
        ORDER BY updated_at DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getByNormalizedUrl(normalizedUrl: String): WebsiteEntryEntity?

    @Query(
        """
        SELECT * FROM website_entries
        WHERE category_id = :categoryId
        ORDER BY sort_order ASC, title COLLATE NOCASE ASC
        """,
    )
    suspend fun getWebsitesByCategory(categoryId: Long): List<WebsiteEntryEntity>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM website_entries WHERE category_id = :categoryId")
    suspend fun maxSortOrderInCategory(categoryId: Long): Int

    @Query(
        """
        SELECT * FROM website_entries
        WHERE last_checked_at IS NULL OR last_checked_at < :staleBefore
        ORDER BY COALESCE(last_checked_at, 0) ASC
        LIMIT :limit
        """,
    )
    suspend fun getEntriesNeedingMetadataRefresh(
        staleBefore: Long,
        limit: Int,
    ): List<WebsiteEntryEntity>

    @Query(
        """
        SELECT * FROM website_entries
        WHERE last_checked_at IS NULL
           OR last_checked_at < :staleBefore
           OR health_status IN ('UNKNOWN', 'TIMEOUT', 'REDIRECTED', 'BLOCKED', 'LOGIN_REQUIRED', 'DNS_FAILED', 'SSL_ISSUE')
        ORDER BY COALESCE(last_checked_at, 0) ASC
        LIMIT :limit
        """,
    )
    suspend fun getEntriesForHealthCheck(
        staleBefore: Long,
        limit: Int,
    ): List<WebsiteEntryEntity>

    @Query(
        """
        UPDATE website_entries
        SET title = :title,
            canonical_url = :canonicalUrl,
            final_url = :finalUrl,
            og_image_url = :ogImageUrl,
            favicon_url = :faviconUrl,
            chosen_icon_source = :chosenIconSource,
            last_checked_at = :lastCheckedAt,
            health_status = :healthStatus,
            updated_at = :updatedAt
        WHERE id = :websiteId
        """,
    )
    suspend fun updateMetadata(
        websiteId: Long,
        title: String,
        canonicalUrl: String?,
        finalUrl: String?,
        ogImageUrl: String?,
        faviconUrl: String?,
        chosenIconSource: IconSource,
        lastCheckedAt: Long,
        healthStatus: HealthStatus,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE website_entries
        SET health_status = :healthStatus,
            last_checked_at = :lastCheckedAt,
            updated_at = :updatedAt
        WHERE id = :websiteId
        """,
    )
    suspend fun updateHealthStatus(
        websiteId: Long,
        healthStatus: HealthStatus,
        lastCheckedAt: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE website_entries
        SET open_count = open_count + 1,
            last_opened_at = :openedAt,
            updated_at = :updatedAt
        WHERE id = :websiteId
        """,
    )
    suspend fun incrementOpenCount(
        websiteId: Long,
        openedAt: Long,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE website_entries
        SET sort_order = :sortOrder,
            category_id = :categoryId,
            updated_at = :updatedAt
        WHERE id = :websiteId
        """,
    )
    suspend fun updateSortOrder(
        websiteId: Long,
        categoryId: Long,
        sortOrder: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE website_entries
        SET is_pinned = :pinned,
            updated_at = :updatedAt
        WHERE id = :websiteId
        """,
    )
    suspend fun updatePinned(
        websiteId: Long,
        pinned: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM website_entries WHERE id = :websiteId")
    suspend fun deleteWebsite(websiteId: Long)

    @Transaction
    @Query(
        """
        SELECT
            w.id AS websiteId,
            w.category_id AS categoryId,
            c.name AS categoryName,
            w.title AS title,
            w.domain AS domain,
            w.normalized_url AS normalizedUrl,
            w.final_url AS finalUrl,
            w.favicon_url AS faviconUrl,
            w.og_image_url AS ogImageUrl,
            w.chosen_icon_source AS chosenIconSource,
            w.custom_icon_uri AS customIconUri,
            w.emoji_icon AS emojiIcon,
            ic.local_uri AS cachedIconUri,
            w.health_status AS healthStatus,
            w.priority AS priority,
            w.follow_up_status AS followUpStatus,
            COALESCE(GROUP_CONCAT(DISTINCT t.name), '') AS tagNames,
            TRIM(
                (CASE WHEN LOWER(w.title) LIKE :queryLike THEN 'title|' ELSE '' END) ||
                (CASE WHEN LOWER(w.domain) LIKE :queryLike THEN 'domain|' ELSE '' END) ||
                (CASE WHEN LOWER(w.normalized_url) LIKE :queryLike THEN 'url|' ELSE '' END) ||
                (CASE WHEN LOWER(COALESCE(w.final_url, '')) LIKE :queryLike THEN 'final|' ELSE '' END) ||
                (CASE WHEN LOWER(c.name) LIKE :queryLike THEN 'category|' ELSE '' END) ||
                (CASE WHEN LOWER(COALESCE(t.name, '')) LIKE :queryLike THEN 'tag|' ELSE '' END),
                '|'
            ) AS matchedFields
        FROM website_entries w
        INNER JOIN categories c ON c.id = w.category_id
        LEFT JOIN icon_cache ic ON ic.website_id = w.id
        LEFT JOIN website_tag_cross_ref wt ON wt.website_id = w.id
        LEFT JOIN tags t ON t.id = wt.tag_id
        WHERE c.is_archived = 0
          AND (
              LOWER(w.title) LIKE :queryLike OR
              LOWER(w.domain) LIKE :queryLike OR
              LOWER(w.normalized_url) LIKE :queryLike OR
              LOWER(COALESCE(w.final_url, '')) LIKE :queryLike OR
              LOWER(c.name) LIKE :queryLike OR
              LOWER(COALESCE(t.name, '')) LIKE :queryLike
          )
        GROUP BY w.id
        ORDER BY w.is_pinned DESC, w.open_count DESC, w.updated_at DESC
        LIMIT :limit
        """,
    )
    suspend fun searchWebsites(
        queryLike: String,
        limit: Int,
    ): List<SearchWebsiteRow>

    @Query(
        """
        SELECT
            w.id AS websiteId,
            w.category_id AS categoryId,
            c.name AS categoryName,
            w.title AS title,
            w.canonical_url AS canonicalUrl,
            w.normalized_url AS normalizedUrl,
            w.final_url AS finalUrl,
            w.domain AS domain
        FROM website_entries w
        INNER JOIN categories c ON c.id = w.category_id
        WHERE (:excludeId IS NULL OR w.id != :excludeId)
          AND (
              w.normalized_url = :normalizedUrl
           OR (:finalUrl IS NOT NULL AND (
                w.final_url = :finalUrl OR
                w.canonical_url = :finalUrl OR
                w.normalized_url = :finalUrl
           ))
           OR (w.domain = :domain AND LOWER(w.title) = LOWER(:title))
          )
        ORDER BY w.updated_at DESC, w.id DESC
        LIMIT 12
        """,
    )
    suspend fun findDuplicateCandidates(
        normalizedUrl: String,
        finalUrl: String?,
        domain: String,
        title: String,
        excludeId: Long? = null,
    ): List<DuplicateWebsiteRow>

    @Query(
        """
        SELECT COUNT(*)
        FROM website_entries
        WHERE health_status IN ('DEAD', 'TIMEOUT', 'DNS_FAILED', 'SSL_ISSUE')
        """,
    )
    suspend fun countBrokenLinks(): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM website_entries w
        WHERE COALESCE(TRIM(w.reason_saved), '') = ''
          AND COALESCE(TRIM(w.note), '') = ''
          AND w.is_pinned = 0
          AND NOT EXISTS (
              SELECT 1 FROM website_tag_cross_ref wt
              WHERE wt.website_id = w.id
          )
        """,
    )
    suspend fun countUnsortedEntries(): Int

    @Query("SELECT MAX(last_checked_at) FROM website_entries")
    suspend fun getLatestHealthCheckTimestamp(): Long?

    @Query(
        """
        SELECT COUNT(*)
        FROM website_entries w
        WHERE w.normalized_url IN (
            SELECT normalized_url
            FROM website_entries
            GROUP BY normalized_url
            HAVING COUNT(*) > 1
        )
        OR (
            w.final_url IS NOT NULL AND w.final_url IN (
                SELECT final_url
                FROM website_entries
                WHERE final_url IS NOT NULL
                GROUP BY final_url
                HAVING COUNT(*) > 1
            )
        )
        OR EXISTS (
            SELECT 1
            FROM website_entries w2
            WHERE w2.id != w.id
              AND w2.domain = w.domain
              AND LOWER(w2.title) = LOWER(w.title)
        )
        """,
    )
    suspend fun countPotentialDuplicates(): Int
}
