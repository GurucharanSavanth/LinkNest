package com.linknest.core.data.usecase

import com.linknest.core.common.time.TimeProvider
import com.linknest.core.data.model.AddWebsiteRequest
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.data.repository.WebsiteRepository
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.network.UrlMetadataFetcher
import com.linknest.core.network.UrlNormalizer
import com.linknest.core.network.model.MetadataResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class AddWebsiteUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository,
    private val urlNormalizer: UrlNormalizer,
    private val metadataFetcher: UrlMetadataFetcher,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(request: AddWebsiteRequest): Result<Long> = try {
        val normalized = urlNormalizer.normalize(request.rawUrl).getOrThrow()
        val metadata = request.preview
            ?.takeIf { it.normalizedUrl == normalized.normalizedUrl }
            ?.toMetadataResult()
            ?: metadataFetcher.fetch(normalized)
        val now = timeProvider.now()
        val finalIconSource = when {
            !request.customIconUri.isNullOrBlank() -> IconSource.CUSTOM
            !request.emojiIcon.isNullOrBlank() -> IconSource.EMOJI
            else -> metadata.chosenIconSource
        }
        val healthStatus = when {
            metadata.finalUrl != normalized.normalizedUrl -> HealthStatus.REDIRECTED
            metadata.ogImageUrl == null && metadata.faviconUrl == null && metadata.title == normalized.domain ->
                HealthStatus.UNKNOWN
            else -> HealthStatus.OK
        }

        Result.success(
            websiteRepository.addWebsite(
                PersistWebsiteRequest(
                    categoryId = request.categoryId,
                    title = metadata.title.ifBlank { normalized.domain },
                    canonicalUrl = metadata.canonicalUrl,
                    finalUrl = metadata.finalUrl,
                    normalizedUrl = normalized.normalizedUrl,
                    domain = normalized.domain,
                    ogImageUrl = metadata.ogImageUrl,
                    faviconUrl = metadata.faviconUrl,
                    chosenIconSource = finalIconSource,
                    customIconUri = request.customIconUri,
                    emojiIcon = request.emojiIcon,
                    tileSizeDp = request.tileSizeDp,
                    sortOrder = Int.MAX_VALUE,
                    isPinned = request.isPinned,
                    healthStatus = healthStatus,
                    createdAt = now,
                    updatedAt = now,
                    lastCheckedAt = now,
                ),
            ),
        )
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

    private fun com.linknest.core.model.MetadataPreview.toMetadataResult(): MetadataResult = MetadataResult(
        title = title,
        canonicalUrl = canonicalUrl,
        finalUrl = normalizedUrl,
        domain = domain,
        ogImageUrl = ogImageUrl,
        faviconUrl = faviconUrl,
        chosenIconSource = chosenIconSource,
    )
}
