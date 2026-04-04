package com.linknest.core.model

data class UserPreferences(
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val tileSizeDp: Int = DEFAULT_TILE_SIZE_DP,
    val tileDensityMode: TileDensityMode = TileDensityMode.ADAPTIVE,
    val backgroundHealthChecksEnabled: Boolean = true,
    val encryptedBackupsEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_TILE_SIZE_DP = 112
    }

    val adaptiveGridMinSizeDp: Int
        get() = when (tileDensityMode) {
            TileDensityMode.COMPACT -> 132
            TileDensityMode.COMFORTABLE -> 180
            TileDensityMode.ADAPTIVE -> tileSizeDp.coerceIn(144, 196)
        }
}

data class SelectableCategory(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconType: IconType,
    val iconValue: String?,
    val suggestionReason: String? = null,
)

data class CategorySuggestion(
    val categoryId: Long,
    val categoryName: String,
    val reason: String,
    val score: Int,
)

data class TagModel(
    val id: Long,
    val name: String,
    val usageCount: Int = 0,
)

data class WebsiteListItem(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val domain: String,
    val normalizedUrl: String,
    val finalUrl: String?,
    val canonicalUrl: String?,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val chosenIconSource: IconSource,
    val customIconUri: String?,
    val emojiIcon: String?,
    val tileSizeDp: Int?,
    val cachedIconUri: String?,
    val isPinned: Boolean,
    val openCount: Int,
    val lastOpenedAt: Long?,
    val lastCheckedAt: Long?,
    val healthStatus: HealthStatus,
    val note: String? = null,
    val reasonSaved: String? = null,
    val priority: WebsitePriority = WebsitePriority.NORMAL,
    val followUpStatus: FollowUpStatus = FollowUpStatus.NONE,
    val revisitAt: Long? = null,
    val sourceLabel: String? = null,
    val customLabel: String? = null,
    val duplicateCount: Int = 0,
    val sortOrder: Int,
    val tagNames: List<String> = emptyList(),
) {
    val preferredIconUrl: String?
        get() = when (chosenIconSource) {
            IconSource.CUSTOM -> customIconUri ?: cachedIconUri ?: faviconUrl
            IconSource.EMOJI -> cachedIconUri ?: faviconUrl
            IconSource.OG_IMAGE -> ogImageUrl ?: faviconUrl
            IconSource.GENERATED,
            -> cachedIconUri ?: faviconUrl ?: ogImageUrl
            else -> cachedIconUri ?: faviconUrl ?: ogImageUrl
        }
}

data class DashboardCategory(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconType: IconType,
    val iconValue: String?,
    val isCollapsed: Boolean,
    val isArchived: Boolean,
    val websites: List<WebsiteListItem>,
) {
    val websiteCount: Int
        get() = websites.size
}

data class DashboardSmartSection(
    val id: String,
    val title: String,
    val websites: List<WebsiteListItem>,
)

data class DashboardModel(
    val categories: List<DashboardCategory> = emptyList(),
    val smartSections: List<DashboardSmartSection> = emptyList(),
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val tileSizeDp: Int = UserPreferences.DEFAULT_TILE_SIZE_DP,
    val tileDensityMode: TileDensityMode = TileDensityMode.ADAPTIVE,
) {
    val pinnedSection: DashboardSmartSection?
        get() = smartSections.firstOrNull { it.id == "pinned" }

    val recentSection: DashboardSmartSection?
        get() = smartSections.firstOrNull { it.id == "recent" }

    val mostUsedSection: DashboardSmartSection?
        get() = smartSections.firstOrNull { it.id == "most_used" }
}

data class MetadataPreview(
    val title: String,
    val normalizedUrl: String,
    val canonicalUrl: String?,
    val finalUrl: String? = null,
    val domain: String,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val chosenIconSource: IconSource,
    val suggestedCategory: CategorySuggestion? = null,
) {
    val preferredIconUrl: String?
        get() = when (chosenIconSource) {
            IconSource.OG_IMAGE -> ogImageUrl ?: faviconUrl
            else -> faviconUrl ?: ogImageUrl
        }
}

data class SearchResultItem(
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
    val priority: WebsitePriority = WebsitePriority.NORMAL,
    val followUpStatus: FollowUpStatus = FollowUpStatus.NONE,
    val tagNames: List<String>,
    val matchedFields: List<String>,
) {
    val preferredIconUrl: String?
        get() = cachedIconUri ?: customIconUri ?: faviconUrl ?: ogImageUrl
}

data class SearchResultGroup(
    val title: String,
    val results: List<SearchResultItem>,
)

data class SearchResultModel(
    val query: String = "",
    val groups: List<SearchResultGroup> = emptyList(),
    val totalCount: Int = 0,
)

data class RecentQuery(
    val id: Long,
    val query: String,
    val useCount: Int,
    val lastUsedAt: Long,
)

data class SearchSuggestion(
    val id: String,
    val type: SearchSuggestionType,
    val title: String,
    val supportingText: String? = null,
    val query: String,
    val savedFilterId: Long? = null,
)

data class WidgetLink(
    val websiteId: Long,
    val title: String,
    val normalizedUrl: String,
)

data class CategoryWidgetSnapshot(
    val categoryId: Long?,
    val categoryName: String,
    val links: List<WidgetLink>,
)

data class CategoryDraft(
    val name: String,
    val colorHex: String,
    val iconType: IconType = IconType.EMOJI,
    val iconValue: String? = null,
)

data class CategoryEditorModel(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconValue: String?,
)

enum class AddWebsitePhase {
    IDLE,
    SECURING_URL,
    INSPECTING_WEBSITE,
    SUGGESTING_CATEGORY,
    RUNNING_SMART_CAPTURE,
    REVIEWING_DUPLICATES,
    RESOLVING_ICON,
    SAVING,
    SUCCESS,
}

data class HealthReportItem(
    val websiteId: Long,
    val title: String,
    val normalizedUrl: String,
    val status: HealthStatus,
    val detailMessage: String? = null,
)

data class SmartCaptureResult(
    val suggestedCategory: CategorySuggestion? = null,
    val suggestedTags: List<String> = emptyList(),
    val cleanedTitle: String? = null,
    val finalUrl: String? = null,
    val iconConfidenceLabel: String? = null,
    val loginRequiredHint: String? = null,
    val suspiciousWarning: String? = null,
    val lowConfidenceWarning: String? = null,
)

data class DuplicateMatch(
    val websiteId: Long,
    val type: DuplicateMatchType,
    val title: String,
    val normalizedUrl: String,
    val categoryId: Long,
    val categoryName: String,
)

data class DuplicateCheckResult(
    val hasDuplicates: Boolean,
    val matches: List<DuplicateMatch>,
) {
    val primaryMatch: DuplicateMatch?
        get() = matches.firstOrNull()
}

data class SavedFilterSpec(
    val query: String = "",
    val categoryIds: List<Long> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val healthStatuses: List<HealthStatus> = emptyList(),
    val pinnedOnly: Boolean = false,
    val recentOnly: Boolean = false,
    val followUpStatuses: List<FollowUpStatus> = emptyList(),
    val priorities: List<WebsitePriority> = emptyList(),
    val duplicatesOnly: Boolean = false,
    val includeNeedsAttention: Boolean = false,
)

data class SavedFilter(
    val id: Long,
    val name: String,
    val spec: SavedFilterSpec,
    val createdAt: Long,
    val updatedAt: Long,
)

data class IntegrityEvent(
    val id: Long,
    val type: IntegrityEventType,
    val title: String,
    val summary: String,
    val successful: Boolean,
    val createdAt: Long,
)

data class IntegrityOverview(
    val lastHealthCheckAt: Long? = null,
    val lastBackupAt: Long? = null,
    val lastRestoreAt: Long? = null,
    val brokenLinksCount: Int = 0,
    val duplicateCount: Int = 0,
    val unsortedCount: Int = 0,
    val cacheSizeBytes: Long = 0L,
    val databaseSizeBytes: Long = 0L,
    val recentEvents: List<IntegrityEvent> = emptyList(),
)
