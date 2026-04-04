package com.linknest.core.database.model

data class DuplicateWebsiteRow(
    val websiteId: Long,
    val categoryId: Long,
    val categoryName: String,
    val title: String,
    val canonicalUrl: String?,
    val normalizedUrl: String,
    val finalUrl: String?,
    val domain: String,
)
