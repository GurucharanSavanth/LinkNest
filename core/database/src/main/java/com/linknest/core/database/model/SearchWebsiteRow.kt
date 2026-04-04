package com.linknest.core.database.model

import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.WebsitePriority

data class SearchWebsiteRow(
    val websiteId: Long,
    val categoryId: Long,
    val categoryName: String,
    val title: String,
    val domain: String,
    val normalizedUrl: String,
    val finalUrl: String?,
    val faviconUrl: String?,
    val ogImageUrl: String?,
    val chosenIconSource: IconSource,
    val customIconUri: String?,
    val emojiIcon: String?,
    val cachedIconUri: String?,
    val healthStatus: HealthStatus,
    val priority: WebsitePriority,
    val followUpStatus: FollowUpStatus,
    val tagNames: String,
    val matchedFields: String,
)
