package com.linknest.core.data.usecase

import com.linknest.core.data.mapper.asPreview
import com.linknest.core.model.MetadataPreview
import com.linknest.core.network.UrlMetadataFetcher
import com.linknest.core.network.UrlNormalizer
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class FetchMetadataPreviewUseCase @Inject constructor(
    private val urlNormalizer: UrlNormalizer,
    private val metadataFetcher: UrlMetadataFetcher,
) {
    suspend operator fun invoke(rawUrl: String): Result<MetadataPreview> = try {
        val normalized = urlNormalizer.normalize(rawUrl).getOrThrow()
        Result.success(metadataFetcher.fetch(normalized).asPreview(normalized.normalizedUrl))
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}
