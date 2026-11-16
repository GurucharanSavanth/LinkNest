package com.linknest.core.data.model

import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.MetadataPreview
import com.linknest.core.model.WebsitePriority

data class AddWebsiteRequest(
    val rawUrl: String,
    val categoryId: Long,
    val customIconUri: String? = null,
    val emojiIcon: String? = null,
    val tileSizeDp: Int? = null,
    val isPinned: Boolean = false,
    val preview: MetadataPreview? = null,
    val tagNames: List<String> = emptyList(),
)

data class PersistWebsiteRequest(
    val existingWebsiteId: Long? = null,
    val categoryId: Long,
    val title: String,
    val canonicalUrl: String?,
    val finalUrl: String?,
    val normalizedUrl: String,
    val domain: String,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val chosenIconSource: IconSource,
    val customIconUri: String?,
    val emojiIcon: String?,
    val tileSizeDp: Int?,
    val sortOrder: Int,
    val isPinned: Boolean,
    val healthStatus: HealthStatus,
    val note: String? = null,
    val reasonSaved: String? = null,
    val priority: WebsitePriority = WebsitePriority.NORMAL,
    val followUpStatus: FollowUpStatus = FollowUpStatus.NONE,
    val revisitAt: Long? = null,
    val sourceLabel: String? = null,
    val customLabel: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastCheckedAt: Long?,
    val tagIds: List<Long> = emptyList(),
    val iconCache: PersistedIconCache? = null,
)

data class PersistedIconCache(
    val sourceUrl: String?,
    val localUri: String?,
    val contentHash: String?,
    val mimeType: String?,
    val fetchedAt: Long,
    val updatedAt: Long,
)

/** Metadata merge fills missing values while preserving the saved link and personal choices. */
fun PersistWebsiteRequest.mergeMetadataInto(existing: com.linknest.core.model.WebsiteListItem): PersistWebsiteRequest {
    val useIncomingIcon = existing.cachedIconUri == null &&
        existing.chosenIconSource !in setOf(IconSource.CUSTOM, IconSource.EMOJI) && iconCache != null
    return copy(
        existingWebsiteId = existing.id,
        categoryId = existing.categoryId, sortOrder = existing.sortOrder,
        normalizedUrl = existing.normalizedUrl, domain = existing.domain, title = existing.title,
        healthStatus = existing.healthStatus, lastCheckedAt = existing.lastCheckedAt,
        canonicalUrl = existing.canonicalUrl ?: canonicalUrl,
        finalUrl = existing.finalUrl ?: finalUrl.takeIf { normalizedUrl == existing.normalizedUrl },
        ogImageUrl = existing.ogImageUrl ?: ogImageUrl, faviconUrl = existing.faviconUrl ?: faviconUrl,
        chosenIconSource = if (useIncomingIcon) chosenIconSource else existing.chosenIconSource,
        iconCache = if (useIncomingIcon) iconCache else null,
        customIconUri = existing.customIconUri, emojiIcon = existing.emojiIcon, tileSizeDp = existing.tileSizeDp,
        isPinned = existing.isPinned, note = existing.note ?: note, reasonSaved = existing.reasonSaved ?: reasonSaved,
        priority = existing.priority, followUpStatus = existing.followUpStatus,
        revisitAt = existing.revisitAt ?: revisitAt,
        sourceLabel = existing.sourceLabel ?: sourceLabel, customLabel = existing.customLabel ?: customLabel,
    )
}

data class DomainCategoryMapping(
    val domain: String,
    val categoryId: Long,
    val usageCount: Int,
    val lastUsedAt: Long,
)

data class CategoryUpsertRequest(
    val categoryId: Long? = null,
    val name: String,
    val colorHex: String,
    val iconType: IconType,
    val iconValue: String?,
)

data class WebsiteHealthCandidate(
    val websiteId: Long,
    val normalizedUrl: String,
    val domain: String,
    val title: String,
)

data class WebsiteHealthUpdate(
    val websiteId: Long,
    val status: HealthStatus,
    val checkedAt: Long,
    val detailMessage: String? = null,
)
