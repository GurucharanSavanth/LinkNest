package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.linknest.core.database.entity.TagEntity
import com.linknest.core.database.entity.WebsiteTagCrossRefEntity
import com.linknest.core.database.model.TagUsageRow
import com.linknest.core.database.model.WebsiteTagAssignmentRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(entity: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<WebsiteTagCrossRefEntity>)

    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               COUNT(wt.website_id) AS usageCount
        FROM tags t
        LEFT JOIN website_tag_cross_ref wt ON wt.tag_id = t.id
        GROUP BY t.id
        ORDER BY LOWER(t.name) ASC
        """,
    )
    fun observeTags(): Flow<List<TagUsageRow>>

    @Query(
        """
        SELECT t.id AS id,
               t.name AS name,
               COUNT(wt.website_id) AS usageCount
        FROM tags t
        LEFT JOIN website_tag_cross_ref wt ON wt.tag_id = t.id
        WHERE LOWER(t.name) LIKE :queryLike
        GROUP BY t.id
        ORDER BY usageCount DESC, LOWER(t.name) ASC
        LIMIT :limit
        """,
    )
    suspend fun searchTags(
        queryLike: String,
        limit: Int,
    ): List<TagUsageRow>

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY LOWER(name) ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM website_tag_cross_ref")
    suspend fun getAllCrossRefs(): List<WebsiteTagCrossRefEntity>

    @Query(
        """
        SELECT wt.website_id AS websiteId,
               t.name AS tagName
        FROM website_tag_cross_ref wt
        INNER JOIN tags t ON t.id = wt.tag_id
        ORDER BY LOWER(t.name) ASC
        """,
    )
    fun observeWebsiteTagAssignments(): Flow<List<WebsiteTagAssignmentRow>>

    @Query(
        """
        SELECT t.name
        FROM website_tag_cross_ref wt
        INNER JOIN tags t ON t.id = wt.tag_id
        WHERE wt.website_id = :websiteId
        ORDER BY LOWER(t.name) ASC
        """,
    )
    suspend fun getTagNamesForWebsite(websiteId: Long): List<String>

    @Query("DELETE FROM website_tag_cross_ref WHERE website_id = :websiteId")
    suspend fun deleteTagsForWebsite(websiteId: Long)

    @Query("DELETE FROM website_tag_cross_ref WHERE website_id = :websiteId AND tag_id = :tagId")
    suspend fun deleteTagFromWebsite(
        websiteId: Long,
        tagId: Long,
    )

    @Query(
        """
        DELETE FROM tags
        WHERE id = :tagId
          AND NOT EXISTS (
              SELECT 1 FROM website_tag_cross_ref
              WHERE website_tag_cross_ref.tag_id = :tagId
          )
        """,
    )
    suspend fun deleteTagIfUnused(tagId: Long): Int
}
