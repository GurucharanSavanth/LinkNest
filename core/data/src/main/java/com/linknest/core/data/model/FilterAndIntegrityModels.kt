package com.linknest.core.data.model

import com.linknest.core.model.IntegrityEventType

data class SavedFilterRecord(
    val id: Long,
    val name: String,
    val specJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class IntegrityEventRecord(
    val id: Long = 0,
    val type: IntegrityEventType,
    val title: String,
    val summary: String,
    val successful: Boolean,
    val createdAt: Long,
)

data class DuplicateWebsiteCandidate(
    val websiteId: Long,
    val categoryId: Long,
    val categoryName: String,
    val title: String,
    val normalizedUrl: String,
    val finalUrl: String?,
    val domain: String,
)

data class WebsiteIntegrityStats(
    val lastHealthCheckAt: Long?,
    val brokenLinksCount: Int,
    val unsortedCount: Int,
)
