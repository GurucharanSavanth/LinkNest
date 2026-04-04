package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.linknest.core.database.entity.IntegrityEventEntity
import com.linknest.core.model.IntegrityEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface IntegrityEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(entity: IntegrityEventEntity): Long

    @Query("SELECT * FROM integrity_events ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<IntegrityEventEntity>

    @Query("SELECT * FROM integrity_events ORDER BY created_at DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<IntegrityEventEntity>>

    @Query(
        """
        SELECT * FROM integrity_events
        WHERE type = :type
        ORDER BY created_at DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestEventByType(type: IntegrityEventType): IntegrityEventEntity?
}
