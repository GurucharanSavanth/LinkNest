package com.linknest.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.linknest.core.database.entity.DomainCategoryMappingEntity
import com.linknest.core.database.model.DomainCategorySuggestionRow

@Dao
interface DomainCategoryMappingDao {
    @Query(
        """
        SELECT m.domain AS domain,
               m.category_id AS categoryId,
               c.name AS categoryName,
               m.usage_count AS usageCount,
               m.last_used_at AS lastUsedAt
        FROM domain_category_mapping m
        INNER JOIN categories c ON c.id = m.category_id
        WHERE m.domain = :domain
          AND c.is_archived = 0
        ORDER BY m.usage_count DESC, m.last_used_at DESC
        LIMIT :limit
        """,
    )
    suspend fun getSuggestions(
        domain: String,
        limit: Int,
    ): List<DomainCategorySuggestionRow>

    @Upsert
    suspend fun upsertMapping(entity: DomainCategoryMappingEntity)

    @Query("SELECT * FROM domain_category_mapping ORDER BY usage_count DESC, last_used_at DESC")
    suspend fun getAllMappings(): List<DomainCategoryMappingEntity>
}
