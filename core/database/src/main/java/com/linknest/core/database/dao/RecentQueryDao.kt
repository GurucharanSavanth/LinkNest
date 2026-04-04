package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.linknest.core.database.entity.RecentQueryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentQueryDao {
    @Query(
        """
        SELECT * FROM recent_queries
        ORDER BY last_used_at DESC, use_count DESC, query COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    fun observeRecentQueries(limit: Int): Flow<List<RecentQueryEntity>>

    @Query(
        """
        SELECT * FROM recent_queries
        ORDER BY last_used_at DESC, use_count DESC, query COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    suspend fun getRecentQueries(limit: Int): List<RecentQueryEntity>

    @Query("SELECT * FROM recent_queries WHERE LOWER(query) = LOWER(:query) LIMIT 1")
    suspend fun getByQuery(query: String): RecentQueryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecentQuery(entity: RecentQueryEntity): Long

    @Update
    suspend fun updateRecentQuery(entity: RecentQueryEntity)

    @Query("DELETE FROM recent_queries")
    suspend fun clearAll()

    @Query("DELETE FROM recent_queries WHERE id = :queryId")
    suspend fun deleteById(queryId: Long)

    @Query(
        """
        DELETE FROM recent_queries
        WHERE id NOT IN (
            SELECT id FROM recent_queries
            ORDER BY last_used_at DESC, use_count DESC, query COLLATE NOCASE ASC
            LIMIT :keepCount
        )
        """,
    )
    suspend fun trimToSize(keepCount: Int)
}
