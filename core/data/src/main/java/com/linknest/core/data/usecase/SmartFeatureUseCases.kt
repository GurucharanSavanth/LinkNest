package com.linknest.core.data.usecase

import android.content.Context
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.IntegrityEventRecord
import com.linknest.core.data.repository.IntegrityEventRepository
import com.linknest.core.data.repository.SavedFilterRepository
import com.linknest.core.data.repository.TagRepository
import com.linknest.core.data.repository.WebsiteRepository
import com.linknest.core.data.storage.LinkNestStorage
import com.linknest.core.model.DuplicateCheckResult
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IntegrityEventType
import com.linknest.core.model.IntegrityOverview
import com.linknest.core.model.MetadataPreview
import com.linknest.core.model.SavedFilter
import com.linknest.core.model.SavedFilterSpec
import com.linknest.core.model.SmartCaptureResult
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.network.model.NormalizedUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RunSmartCaptureUseCase @Inject constructor(
    private val tagRepository: TagRepository,
) {
    suspend operator fun invoke(
        normalizedUrl: NormalizedUrl,
        metadataPreview: MetadataPreview,
        suggestedCategory: com.linknest.core.model.CategorySuggestion?,
    ): SmartCaptureResult {
        val cleanedTitle = metadataPreview.title
            .replace(Regex("\\s*[|\\-•]\\s*${Regex.escape(normalizedUrl.domain)}\$"), "")
            .trim()
            .ifBlank { metadataPreview.title }
        val candidateTokens = buildSet {
            cleanedTitle.split(Regex("[^A-Za-z0-9]+"))
                .map(String::trim)
                .filter { it.length >= 3 }
                .take(6)
                .forEach { add(it) }
            normalizedUrl.domain.split('.')
                .filter { it.length >= 3 && it != "www" }
                .forEach { add(it) }
        }
        val suggestedTags = candidateTokens
            .flatMap { token -> tagRepository.searchTags(token, limit = 4) }
            .map { it.name }
            .distinct()
            .take(6)

        val finalUrl = metadataPreview.finalUrl ?: metadataPreview.normalizedUrl
        val loginHint = when {
            cleanedTitle.contains("login", ignoreCase = true) ||
                cleanedTitle.contains("sign in", ignoreCase = true) ||
                finalUrl.contains("login", ignoreCase = true) -> "This destination looks login-gated."
            else -> null
        }

        val lowConfidenceWarning = when {
            metadataPreview.faviconUrl.isNullOrBlank() &&
                metadataPreview.ogImageUrl.isNullOrBlank() &&
                cleanedTitle.equals(normalizedUrl.domain, ignoreCase = true) ->
                "Low-confidence capture. Metadata was minimal."
            else -> null
        }

        return SmartCaptureResult(
            suggestedCategory = suggestedCategory,
            suggestedTags = suggestedTags,
            cleanedTitle = cleanedTitle.takeIf { it != metadataPreview.title },
            finalUrl = finalUrl.takeIf { it != normalizedUrl.normalizedUrl },
            iconConfidenceLabel = when (metadataPreview.chosenIconSource) {
                IconSource.OG_IMAGE -> "High confidence icon"
                IconSource.REL_ICON,
                IconSource.APPLE_TOUCH_ICON,
                -> "Good confidence icon"
                IconSource.FAVICON_FALLBACK -> "Fallback icon"
                IconSource.CUSTOM -> "Custom icon"
                IconSource.EMOJI -> "Emoji icon"
                IconSource.GENERATED -> "Generated icon"
            },
            loginRequiredHint = loginHint,
            suspiciousWarning = if (normalizedUrl.isInternationalizedHost) {
                "Internationalized host was normalized to ASCII for safer handling."
            } else {
                null
            },
            lowConfidenceWarning = lowConfidenceWarning,
        )
    }
}

class CheckDuplicateUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        normalizedUrl: String,
        finalUrl: String?,
        domain: String,
        title: String,
        excludeWebsiteId: Long? = null,
    ): DuplicateCheckResult = websiteRepository.findDuplicates(
        normalizedUrl = normalizedUrl,
        finalUrl = finalUrl,
        domain = domain,
        title = title,
        excludeWebsiteId = excludeWebsiteId,
    )
}

class SaveFilterUseCase @Inject constructor(
    private val savedFilterRepository: SavedFilterRepository,
) {
    suspend operator fun invoke(
        name: String,
        spec: SavedFilterSpec,
    ): SavedFilter = savedFilterRepository.saveFilter(name, spec)
}

class LoadSavedFiltersUseCase @Inject constructor(
    private val savedFilterRepository: SavedFilterRepository,
) {
    operator fun invoke(): Flow<List<SavedFilter>> = savedFilterRepository.observeSavedFilters()
}

class DeleteSavedFilterUseCase @Inject constructor(
    private val savedFilterRepository: SavedFilterRepository,
) {
    suspend operator fun invoke(filterId: Long) {
        savedFilterRepository.deleteSavedFilter(filterId)
    }
}

class ApplyFilterUseCase @Inject constructor() {
    operator fun invoke(
        items: List<WebsiteListItem>,
        spec: SavedFilterSpec,
    ): List<WebsiteListItem> {
        val normalizedQuery = spec.query.trim().lowercase()
        return items.filter { item ->
            (normalizedQuery.isBlank() || listOfNotNull(
                item.title,
                item.domain,
                item.normalizedUrl,
                item.finalUrl,
                item.note,
                item.reasonSaved,
                item.sourceLabel,
                item.customLabel,
            ).any { candidate -> candidate.lowercase().contains(normalizedQuery) }) &&
                (spec.categoryIds.isEmpty() || item.categoryId in spec.categoryIds) &&
                (spec.tagNames.isEmpty() || spec.tagNames.any { tag ->
                    item.tagNames.any { it.equals(tag, ignoreCase = true) }
                }) &&
                (spec.healthStatuses.isEmpty() || item.healthStatus in spec.healthStatuses) &&
                (!spec.pinnedOnly || item.isPinned) &&
                (!spec.recentOnly || item.lastOpenedAt != null) &&
                (spec.followUpStatuses.isEmpty() || item.followUpStatus in spec.followUpStatuses) &&
                (spec.priorities.isEmpty() || item.priority in spec.priorities) &&
                (!spec.duplicatesOnly || item.duplicateCount > 0) &&
                (!spec.includeNeedsAttention || item.healthStatus in setOf(
                    HealthStatus.BLOCKED,
                    HealthStatus.LOGIN_REQUIRED,
                    HealthStatus.REDIRECTED,
                    HealthStatus.DNS_FAILED,
                    HealthStatus.SSL_ISSUE,
                    HealthStatus.DEAD,
                    HealthStatus.TIMEOUT,
                ))
        }
    }
}

class GetRecentUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(limit: Int = 8): List<WebsiteListItem> =
        websiteRepository.getRecent(limit)
}

class GetMostUsedUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(limit: Int = 8): List<WebsiteListItem> =
        websiteRepository.getMostUsed(limit)
}

class GetNeedsAttentionUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(limit: Int = 8): List<WebsiteListItem> =
        websiteRepository.getNeedsAttention(limit)
}

class GetDashboardSmartSectionsUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(): Map<String, List<WebsiteListItem>> = mapOf(
        "recent" to websiteRepository.getRecent(6),
        "most_used" to websiteRepository.getMostUsed(6),
        "needs_attention" to websiteRepository.getNeedsAttention(6),
        "duplicates" to websiteRepository.getPotentialDuplicates(6),
        "unsorted" to websiteRepository.getUnsorted(6),
        "last_imported" to websiteRepository.getLastImported(6),
    ).filterValues { it.isNotEmpty() }
}

class RecordIntegrityEventUseCase @Inject constructor(
    private val integrityEventRepository: IntegrityEventRepository,
) {
    suspend operator fun invoke(record: IntegrityEventRecord) {
        integrityEventRepository.recordEvent(record)
    }
}

class GetIntegrityOverviewUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
    private val integrityEventRepository: IntegrityEventRepository,
    @ApplicationContext private val appContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): IntegrityOverview = withContext(ioDispatcher) {
        val cacheDir = LinkNestStorage.iconCacheDirectory(appContext)
        val databaseFile = appContext.getDatabasePath("linknest.db")
        IntegrityOverview(
            lastHealthCheckAt = websiteRepository.getLatestHealthCheckTimestamp(),
            lastBackupAt = integrityEventRepository.getLatestEventTimestampByType(IntegrityEventType.BACKUP_EXPORT),
            lastRestoreAt = integrityEventRepository.getLatestEventTimestampByType(IntegrityEventType.RESTORE_IMPORT),
            brokenLinksCount = websiteRepository.getIntegrityBrokenLinksCount(),
            duplicateCount = websiteRepository.getIntegrityDuplicateCount(),
            unsortedCount = websiteRepository.getIntegrityUnsortedCount(),
            cacheSizeBytes = cacheDir.takeIf(File::exists)?.walkTopDown()?.filter(File::isFile)?.sumOf(File::length) ?: 0L,
            databaseSizeBytes = databaseFile.takeIf(File::exists)?.length() ?: 0L,
            recentEvents = integrityEventRepository.getRecentEvents(8),
        )
    }
}
