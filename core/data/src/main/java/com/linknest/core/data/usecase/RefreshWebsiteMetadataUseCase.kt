package com.linknest.core.data.usecase

import com.linknest.core.data.repository.WebsiteRepository
import javax.inject.Inject

class RefreshWebsiteMetadataUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
) {
    suspend operator fun invoke(
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
        limit: Int = DEFAULT_LIMIT,
    ): Int {
        val staleBefore = System.currentTimeMillis() - staleAfterMillis
        return websiteRepository.refreshStaleMetadata(staleBefore = staleBefore, limit = limit)
    }

    private companion object {
        const val DEFAULT_LIMIT = 20
        const val DEFAULT_STALE_AFTER_MILLIS = 24L * 60L * 60L * 1000L
    }
}
