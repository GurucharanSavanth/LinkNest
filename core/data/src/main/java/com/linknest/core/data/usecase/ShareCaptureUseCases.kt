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
        // Bound the scan before running the regex: shared text can be arbitrarily large (a whole
        // article body), and scanning all of it on the caller's thread risks an ANR. A real share
        // places the URL near the front, and a single URL can't exceed MAX_URL_LENGTH (8192), so a
        // generous prefix is enough. ponytail: prefix scan; widen MAX_SCAN_CHARS if a real share
        // ever buries its only URL past 64 KiB.
        val normalizedText = sharedText
            .take(MAX_SCAN_CHARS)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
        if (normalizedText.isBlank()) return null

        return URL_REGEX.findAll(normalizedText)
            .map { match -> match.value.trimEnd('.', ',', ';', ')', ']', '}') }
            .maxByOrNull(String::length)
    }

    private companion object {
        const val MAX_SCAN_CHARS = 64 * 1024
        val URL_REGEX = Regex(
            pattern = """(?i)\b((?:https?://|www\.)[^\s<>()]+|(?:[a-z0-9-]+\.)+[a-z]{2,}(?:/[^\s<>()]*)?)""",
        )
    }
}
