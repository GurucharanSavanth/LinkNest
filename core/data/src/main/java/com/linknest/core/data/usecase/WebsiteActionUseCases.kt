package com.linknest.core.data.usecase

import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.model.WebsiteHealthCandidate
import com.linknest.core.data.model.WebsiteHealthUpdate
import com.linknest.core.data.repository.SearchIndexRepository
import com.linknest.core.data.repository.WebsiteRepository
import com.linknest.core.model.SearchResultItem
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.network.UrlHealthMonitor
import javax.inject.Inject

class SearchUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
    private val searchIndexRepository: SearchIndexRepository,
) {
    suspend operator fun invoke(
        query: String,
        limit: Int = 40,
    ): List<SearchResultItem> {
        val sanitizedQuery = query.trim()
        if (sanitizedQuery.isBlank()) return emptyList()
        val indexedResults = searchIndexRepository.search(
            query = sanitizedQuery,
            limit = limit,
        )
        return if (indexedResults.isNotEmpty()) {
            indexedResults
        } else {
            websiteRepository.search(sanitizedQuery, limit)
        }
    }
}

class GetWebsiteUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(websiteId: Long): WebsiteListItem? =
        websiteRepository.getWebsiteById(websiteId)
}

class GetAllWebsiteItemsUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(): List<WebsiteListItem> =
        websiteRepository.getAllWebsiteItems()
}

class CheckWebsiteHealthUseCase @Inject constructor(
    private val urlHealthMonitor: UrlHealthMonitor,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(candidate: WebsiteHealthCandidate): WebsiteHealthUpdate =
        urlHealthMonitor.probe(candidate.normalizedUrl).let { status ->
            WebsiteHealthUpdate(
                websiteId = candidate.websiteId,
                status = status,
                checkedAt = timeProvider.now(),
                detailMessage = when (status) {
                    com.linknest.core.model.HealthStatus.OK -> "Reachable"
                    com.linknest.core.model.HealthStatus.REDIRECTED -> "Redirected to a different destination"
                    com.linknest.core.model.HealthStatus.BLOCKED -> "Blocked or restricted in this region"
                    com.linknest.core.model.HealthStatus.LOGIN_REQUIRED -> "Authentication is required"
                    com.linknest.core.model.HealthStatus.DNS_FAILED -> "DNS resolution failed"
                    com.linknest.core.model.HealthStatus.SSL_ISSUE -> "TLS or certificate validation failed"
                    com.linknest.core.model.HealthStatus.DEAD -> "Returned a broken response"
                    com.linknest.core.model.HealthStatus.TIMEOUT -> "Timed out"
                    com.linknest.core.model.HealthStatus.UNKNOWN -> "Status could not be determined"
                },
            )
        }
}

class PersistHealthStatusesUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(updates: List<WebsiteHealthUpdate>) {
        websiteRepository.updateHealthStatuses(updates)
    }
}

class GetHealthCheckCandidatesUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        staleBefore: Long,
        limit: Int = 20,
    ): List<WebsiteHealthCandidate> = websiteRepository.getHealthCheckCandidates(
        staleBefore = staleBefore,
        limit = limit,
    )
}

class ReorderWebsiteUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        categoryId: Long,
        orderedIds: List<Long>,
    ) {
        websiteRepository.reorderWebsites(categoryId, orderedIds)
    }
}

class TrackWebsiteOpenUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(websiteId: Long) {
        websiteRepository.trackOpen(websiteId)
    }
}

class MoveWebsiteToCategoryUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        websiteId: Long,
        targetCategoryId: Long,
    ) {
        websiteRepository.moveWebsiteToCategory(websiteId, targetCategoryId)
    }
}

class DeleteWebsiteUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(websiteId: Long) {
        websiteRepository.deleteWebsite(websiteId)
    }
}

class SetWebsitePinnedUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        websiteId: Long,
        pinned: Boolean,
    ) {
        websiteRepository.setPinned(websiteId, pinned)
    }
}
