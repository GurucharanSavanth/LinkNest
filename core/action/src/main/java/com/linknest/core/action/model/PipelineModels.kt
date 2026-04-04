package com.linknest.core.action.model

import com.linknest.core.data.model.BackupArtifact
import com.linknest.core.data.model.ImportSummary
import com.linknest.core.data.model.PersistedIconCache
import com.linknest.core.data.model.WebsiteHealthUpdate
import com.linknest.core.model.CategorySuggestion
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.DuplicateDecision
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthReportItem
import com.linknest.core.model.IconSource
import com.linknest.core.model.IntegrityOverview
import com.linknest.core.model.MetadataPreview
import com.linknest.core.model.SearchResultGroup
import com.linknest.core.model.SmartCaptureResult
import com.linknest.core.model.WebsitePriority

data class AddWebsitePipelineInput(
    val existingWebsiteId: Long? = null,
    val rawUrl: String,
    val selectedCategoryId: Long?,
    val customIconUri: String? = null,
    val emojiIcon: String? = null,
    val tileSizeDp: Int? = null,
    val isPinned: Boolean = false,
    val preview: MetadataPreview? = null,
    val tagNames: List<String> = emptyList(),
    val note: String? = null,
    val reasonSaved: String? = null,
    val priority: WebsitePriority = WebsitePriority.NORMAL,
    val followUpStatus: FollowUpStatus = FollowUpStatus.NONE,
    val revisitAt: Long? = null,
    val sourceLabel: String? = null,
    val customLabel: String? = null,
    val duplicateDecision: DuplicateDecision? = null,
)

data class AddWebsitePipelineOutput(
    val websiteId: Long?,
    val categoryId: Long,
    val normalizedUrl: String,
    val metadataPreview: MetadataPreview,
    val categorySuggestion: CategorySuggestion?,
    val smartCapture: SmartCaptureResult,
    val duplicateCheck: DuplicateCheckResult?,
    val cachedIconUri: String?,
    val requiresDuplicateDecision: Boolean,
    val wasPersisted: Boolean,
)

data class SearchPipelineInput(
    val query: String,
    val limit: Int = 40,
    val groupByCategory: Boolean = true,
)

data class SearchPipelineOutput(
    val query: String,
    val groups: List<SearchResultGroup>,
    val totalCount: Int,
)

data class ShareCapturePipelineInput(
    val sharedText: String,
)

data class ShareCapturePipelineOutput(
    val normalizedUrl: String,
)

data class HealthCheckPipelineOutput(
    val checkedCount: Int,
    val okCount: Int,
    val blockedCount: Int,
    val redirectedCount: Int,
    val deadCount: Int,
    val timeoutCount: Int,
    val loginRequiredCount: Int = 0,
    val dnsFailedCount: Int = 0,
    val sslIssueCount: Int = 0,
    val items: List<HealthReportItem>,
)

data class HealthCheckProgress(
    val totalCount: Int,
    val completedCount: Int,
    val latestItem: HealthReportItem?,
)

data class BackupExportPipelineInput(
    val encrypted: Boolean,
)

data class BackupExportPipelineOutput(
    val artifact: BackupArtifact,
)

data class ImportRestorePipelineOutput(
    val summary: ImportSummary,
)

data class IconResolutionInput(
    val customIconUri: String?,
    val emojiIcon: String?,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val preferredSource: IconSource,
    val fetchedAt: Long,
)

data class IconResolutionOutput(
    val chosenIconSource: IconSource,
    val sourceUrl: String?,
    val persistedIconCache: PersistedIconCache?,
)

data class SuggestCategoryInput(
    val domain: String,
    val contextHint: String? = null,
)

data class SmartCaptureInput(
    val normalizedUrl: com.linknest.core.network.model.NormalizedUrl,
    val preview: MetadataPreview,
    val suggestedCategory: CategorySuggestion?,
)

data class DuplicateCheckInput(
    val normalizedUrl: String,
    val finalUrl: String?,
    val domain: String,
    val title: String,
    val excludeWebsiteId: Long? = null,
)

data class UpdateDomainMappingInput(
    val domain: String,
    val categoryId: Long,
)

data class ReorderWebsiteInput(
    val categoryId: Long,
    val orderedIds: List<Long>,
)

data class IntegrityOverviewInput(
    val refresh: Boolean = false,
)

data class IntegrityOverviewOutput(
    val overview: IntegrityOverview,
)
