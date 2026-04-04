package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.linknest.core.database.entity.SavedFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY updated_at DESC, name COLLATE NOCASE ASC")
    fun observeSavedFilters(): Flow<List<SavedFilterEntity>>

    @Query("SELECT * FROM saved_filters ORDER BY updated_at DESC, name COLLATE NOCASE ASC")
    suspend fun getSavedFilters(): List<SavedFilterEntity>

    @Query("SELECT * FROM saved_filters WHERE id = :filterId LIMIT 1")
    suspend fun getSavedFilterById(filterId: Long): SavedFilterEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSavedFilter(entity: SavedFilterEntity): Long

    @Update
    suspend fun updateSavedFilter(entity: SavedFilterEntity)

    @Query("DELETE FROM saved_filters WHERE id = :filterId")
    suspend fun deleteSavedFilter(filterId: Long)
}
