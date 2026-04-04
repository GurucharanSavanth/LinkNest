package com.linknest.core.data.repository

import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.mapper.asCategorySuggestion
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.database.dao.CategoryDao
import com.linknest.core.database.dao.DomainCategoryMappingDao
import com.linknest.core.database.entity.DomainCategoryMappingEntity
import com.linknest.core.model.CategorySuggestion
import javax.inject.Inject

class OfflineFirstDomainCategoryMappingRepository @Inject constructor(
    private val mappingDao: DomainCategoryMappingDao,
    private val categoryDao: CategoryDao,
    private val timeProvider: TimeProvider,
) : DomainCategoryMappingRepository {
    override suspend fun suggestCategory(
        domain: String,
        contextHint: String?,
    ): CategorySuggestion? {
        val normalizedDomain = domain.trim().lowercase()
        if (normalizedDomain.isBlank()) return null

        val historySuggestion = mappingDao.getSuggestions(
            domain = normalizedDomain,
            limit = 1,
        ).firstOrNull()?.asCategorySuggestion()
        if (historySuggestion != null) return historySuggestion

        val categories = categoryDao.getActiveCategories()
        val haystack = "$normalizedDomain ${contextHint.orEmpty()}".lowercase()
        return categories
            .mapNotNull { category ->
                val categoryName = category.name.lowercase()
                val categoryTokens = categoryName.split(" ", "-", "_")
                val baseScore = when {
                    categoryName in haystack -> 80
                    categoryTokens.any { token -> token.length > 2 && token in haystack } -> 55
                    categoryName == "inbox" -> 5
                    else -> keywordScore(categoryName, haystack)
                }
                if (baseScore <= 0) {
                    null
                } else {
                    CategorySuggestion(
                        categoryId = category.id,
                        categoryName = category.name,
                        reason = "Keyword match from URL context",
                        score = baseScore,
                    )
                }
            }
            .maxByOrNull { suggestion -> suggestion.score }
    }

    override suspend fun recordUsage(domain: String, categoryId: Long) {
        val normalizedDomain = domain.trim().lowercase()
        if (normalizedDomain.isBlank()) return
        val now = timeProvider.now()
        val existing = mappingDao.getSuggestions(
            domain = normalizedDomain,
            limit = 50,
        ).firstOrNull { row -> row.categoryId == categoryId }
        mappingDao.upsertMapping(
            DomainCategoryMappingEntity(
                domain = normalizedDomain,
                categoryId = categoryId,
                usageCount = (existing?.usageCount ?: 0) + 1,
                lastUsedAt = now,
            ),
        )
    }

    override suspend fun getAllMappings(): List<DomainCategoryMapping> =
        mappingDao.getAllMappings().map { entity ->
            DomainCategoryMapping(
                domain = entity.domain,
                categoryId = entity.categoryId,
                usageCount = entity.usageCount,
                lastUsedAt = entity.lastUsedAt,
            )
        }

    private fun keywordScore(
        categoryName: String,
        haystack: String,
    ): Int = when {
        categoryName.contains("tech") && haystack.anyKeyword("github", "kotlin", "android", "dev", "stack") -> 45
        categoryName.contains("travel") && haystack.anyKeyword("trip", "flight", "hotel", "booking", "travel") -> 45
        categoryName.contains("design") && haystack.anyKeyword("figma", "dribbble", "behance", "design") -> 45
        categoryName.contains("finance") && haystack.anyKeyword("bank", "invest", "finance", "billing", "money") -> 45
        else -> 0
    }

    private fun String.anyKeyword(vararg keywords: String): Boolean = keywords.any { keyword ->
        keyword in this
    }
}
