package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.linknest.core.database.entity.IconCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IconCacheDao {
    @Upsert
    suspend fun upsertIconCache(entity: IconCacheEntity)

    @Query("SELECT * FROM icon_cache WHERE website_id = :websiteId LIMIT 1")
    suspend fun getByWebsiteId(websiteId: Long): IconCacheEntity?

    @Query("SELECT * FROM icon_cache WHERE source_url = :sourceUrl LIMIT 1")
    suspend fun getBySourceUrl(sourceUrl: String): IconCacheEntity?

    @Query("SELECT * FROM icon_cache")
    fun observeAll(): Flow<List<IconCacheEntity>>

    @Query("SELECT * FROM icon_cache")
    suspend fun getAll(): List<IconCacheEntity>

    @Query("DELETE FROM icon_cache WHERE website_id = :websiteId")
    suspend fun deleteByWebsiteId(websiteId: Long)

    // icon_cache has no FK to website_entries, so a category delete (which cascades its websites)
    // would otherwise orphan these rows. Clear them explicitly before the cascade.
    @Query(
        "DELETE FROM icon_cache WHERE website_id IN " +
            "(SELECT id FROM website_entries WHERE category_id = :categoryId)",
    )
    suspend fun deleteByCategoryId(categoryId: Long)

    @Query("DELETE FROM icon_cache")
    suspend fun deleteAll()
}
