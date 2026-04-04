package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.linknest.core.database.entity.CategoryEntity
import com.linknest.core.database.model.CategoryWithWebsitesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM categories
        WHERE is_archived = 0
        ORDER BY sort_order ASC, name COLLATE NOCASE ASC
        """,
    )
    fun observeCategoryWithWebsites(): Flow<List<CategoryWithWebsitesEntity>>

    @Query(
        """
        SELECT * FROM categories
        WHERE is_archived = 0
        ORDER BY sort_order ASC, name COLLATE NOCASE ASC
        """,
    )
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM categories
        WHERE is_archived = 0
        ORDER BY sort_order ASC, name COLLATE NOCASE ASC
        """,
    )
    suspend fun getActiveCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(entity: CategoryEntity): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM categories")
    suspend fun maxSortOrder(): Int

    @Query(
        """
        UPDATE categories
        SET sort_order = :sortOrder,
            updated_at = :updatedAt
        WHERE id = :categoryId
        """,
    )
    suspend fun updateSortOrder(
        categoryId: Long,
        sortOrder: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE categories
        SET name = :name,
            color_hex = :colorHex,
            icon_type = :iconType,
            icon_value = :iconValue,
            updated_at = :updatedAt
        WHERE id = :categoryId
        """,
    )
    suspend fun updateCategory(
        categoryId: Long,
        name: String,
        colorHex: String,
        iconType: com.linknest.core.model.IconType,
        iconValue: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE categories
        SET is_archived = :archived,
            updated_at = :updatedAt
        WHERE id = :categoryId
        """,
    )
    suspend fun setArchived(
        categoryId: Long,
        archived: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: Long)

    @Query(
        """
        UPDATE categories
        SET is_collapsed = CASE WHEN is_collapsed = 1 THEN 0 ELSE 1 END,
            updated_at = :updatedAt
        WHERE id = :categoryId
        """,
    )
    suspend fun toggleCollapsed(categoryId: Long, updatedAt: Long)
}
