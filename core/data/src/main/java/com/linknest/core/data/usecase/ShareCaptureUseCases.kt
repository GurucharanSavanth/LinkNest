package com.linknest.core.data.usecase

import com.linknest.core.network.UrlNormalizer
import com.linknest.core.network.model.NormalizedUrl
import javax.inject.Inject

class HandleIncomingShareUseCase @Inject constructor(
    private val urlNormalizer: UrlNormalizer,
) {
    fun invoke(sharedText: String): Result<NormalizedUrl> = runCatching {
        val extractedUrl = extractDominantUrl(sharedText)
            ?: error("No usable website URL was found in the shared content.")
        urlNormalizer.normalize(extractedUrl).getOrThrow()
    }

    private fun extractDominantUrl(sharedText: String): String? {
        val normalizedText = sharedText
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
        if (normalizedText.isBlank()) return null

        return URL_REGEX.findAll(normalizedText)
            .map { match -> match.value.trimEnd('.', ',', ';', ')', ']', '}') }
            .maxByOrNull(String::length)
    }

    private companion object {
        val URL_REGEX = Regex(
            pattern = """(?i)\b((?:https?://|www\.)[^\s<>()]+|(?:[a-z0-9-]+\.)+[a-z]{2,}(?:/[^\s<>()]*)?)""",
        )
    }
}
