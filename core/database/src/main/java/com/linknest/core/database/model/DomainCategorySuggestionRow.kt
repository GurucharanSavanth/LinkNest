package com.linknest.core.database.model

data class DomainCategorySuggestionRow(
    val domain: String,
    val categoryId: Long,
    val categoryName: String,
    val usageCount: Int,
    val lastUsedAt: Long,
)
