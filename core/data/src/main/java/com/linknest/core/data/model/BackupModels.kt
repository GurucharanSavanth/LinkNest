package com.linknest.core.data.model

import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.IntegrityEventType
import com.linknest.core.model.WebsitePriority

data class BackupSnapshot(
    val schemaVersion: Int = 2,
    val exportedAt: Long,
    val categories: List<BackupCategory>,
    val websites: List<BackupWebsite>,
    val tags: List<BackupTag>,
    val websiteTags: List<BackupWebsiteTag>,
    val mappings: List<DomainCategoryMapping>,
    val savedFilters: List<BackupSavedFilter> = emptyList(),
    val events: List<BackupIntegrityEvent> = emptyList(),
)

data class BackupCategory(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconType: IconType,
    val iconValue: String?,
    val sortOrder: Int,
    val isCollapsed: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BackupWebsite(
    val id: Long,
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
    val openCount: Int,
    val lastOpenedAt: Long?,
    val lastCheckedAt: Long?,
    val healthStatus: HealthStatus,
    val note: String?,
    val reasonSaved: String?,
    val priority: WebsitePriority,
    val followUpStatus: FollowUpStatus,
    val revisitAt: Long?,
    val sourceLabel: String?,
    val customLabel: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BackupTag(
    val id: Long,
    val name: String,
)

data class BackupWebsiteTag(
    val websiteId: Long,
    val tagId: Long,
)

data class BackupSavedFilter(
    val id: Long,
    val name: String,
    val specJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BackupIntegrityEvent(
    val id: Long,
    val type: IntegrityEventType,
    val title: String,
    val summary: String,
    val successful: Boolean,
    val createdAt: Long,
)

data class BackupArtifact(
    val fileName: String,
    val filePath: String,
    val json: String,
    val isEncrypted: Boolean,
)

data class ImportSummary(
    val importedCategories: Int,
    val importedWebsites: Int,
    val importedTags: Int,
    val importedMappings: Int,
    val importedSavedFilters: Int = 0,
    val importedEvents: Int = 0,
    val skippedWebsites: Int,
    val warnings: List<String> = emptyList(),
)
