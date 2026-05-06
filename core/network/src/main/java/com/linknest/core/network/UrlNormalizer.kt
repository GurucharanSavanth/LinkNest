package com.linknest.core.network

import com.linknest.core.network.model.NormalizedUrl
import java.net.URI
import javax.inject.Inject

class UrlNormalizer @Inject constructor() {
    fun normalize(rawUrl: String): Result<NormalizedUrl> = runCatching {
        val candidate = rawUrl.trim()
        require(candidate.isNotBlank()) { "URL is required." }
        require(candidate.length <= MAX_URL_LENGTH) { "URL exceeds the supported length." }

        val withScheme = if (SCHEME_PATTERN.containsMatchIn(candidate)) {
            candidate
        } else {
            "https://$candidate"
        }

        val parsed = URI(withScheme)
        val originalScheme = parsed.scheme?.lowercase() ?: error("Missing URL scheme.")
        require(originalScheme !in BLOCKED_SCHEMES) { "This URL scheme is unsafe." }

        val normalizedResult = if (originalScheme == "http" || originalScheme == "https") {
            normalizeWebUrl(rawUrl, parsed, originalScheme)
        } else {
            normalizeExternalScheme(rawUrl, candidate, parsed, originalScheme)
        }

        normalizedResult
    }

    private fun normalizeWebUrl(
        rawUrl: String,
        parsed: URI,
        originalScheme: String,
    ): NormalizedUrl {
        require(parsed.userInfo.isNullOrBlank()) { "Credentials in URLs are not supported." }
        require(parsed.rawAuthority?.contains('@') != true) { "Credentials in URLs are not supported." }

        val host = parsed.host?.trim()
            ?: parsed.rawAuthority?.substringBefore('@')?.substringBefore(':')?.trim()
            ?: error("URL must include a valid host.")
        val (asciiHost, isInternationalizedHost) = UrlSecurityPolicy.normalizeHost(host)

        val path = parsed.path?.ifBlank { "/" } ?: "/"
        val normalizedPort = parsed.port
            .takeUnless { it == 80 || it == 443 }
            ?: -1
        val normalized = URI(
            "https",
            null,
            asciiHost,
            normalizedPort,
            path,
            parsed.query,
            parsed.fragment,
        ).toString()

        return NormalizedUrl(
            rawInput = rawUrl,
            normalizedUrl = normalized,
            host = asciiHost,
            domain = asciiHost.removePrefix("www."),
            wasInsecureSchemeUpgraded = originalScheme == "http",
            isInternationalizedHost = isInternationalizedHost,
        )
    }

    private fun normalizeExternalScheme(
        rawUrl: String,
        candidate: String,
        parsed: URI,
        originalScheme: String,
    ): NormalizedUrl {
        val host = parsed.host?.trim().orEmpty()
        val normalizedHost = host.ifBlank { originalScheme }
        return NormalizedUrl(
            rawInput = rawUrl,
            normalizedUrl = candidate,
            host = normalizedHost,
            domain = normalizedHost.removePrefix("www."),
            hasUnsupportedScheme = true,
        )
    }

    private companion object {
        val SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z\\d+.-]*:")
        val BLOCKED_SCHEMES = setOf("file", "javascript", "data", "content")
        const val MAX_URL_LENGTH = 8192
    }
}
