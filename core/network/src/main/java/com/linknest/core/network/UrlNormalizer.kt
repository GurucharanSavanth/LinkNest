package com.linknest.core.network

import com.linknest.core.network.model.NormalizedUrl
import java.net.URI
import javax.inject.Inject

class UrlNormalizer @Inject constructor() {
    fun normalize(rawUrl: String): Result<NormalizedUrl> = runCatching {
        normalizeInternal(rawUrl, strict = false)
    }

    fun normalizeStrict(rawUrl: String): Result<NormalizedUrl> = runCatching {
        normalizeInternal(rawUrl, strict = true)
    }

    private fun normalizeInternal(rawUrl: String, strict: Boolean): NormalizedUrl {
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

        if (strict) {
            require(originalScheme !in BLOCKED_SCHEMES) { "This URL scheme is unsafe." }
        }

        val normalizedResult = if (originalScheme == "http" || originalScheme == "https") {
            normalizeWebUrl(rawUrl, parsed, originalScheme, strict)
        } else {
            normalizeExternalScheme(rawUrl, candidate, parsed, originalScheme)
        }

        return normalizedResult
    }

    private fun normalizeWebUrl(
        rawUrl: String,
        parsed: URI,
        originalScheme: String,
        strict: Boolean,
    ): NormalizedUrl {
        if (strict) {
            require(parsed.userInfo.isNullOrBlank()) { "Credentials in URLs are not supported." }
            require(parsed.rawAuthority?.contains('@') != true) { "Credentials in URLs are not supported." }
        }

        val host = parsed.host?.trim()
            ?: parsed.rawAuthority?.substringBefore('@')?.substringBefore(':')?.trim()
            ?: if (strict) error("URL must include a valid host.") else rawUrl

        val (asciiHost, isInternationalizedHost) = if (strict) {
            UrlSecurityPolicy.normalizeHost(host)
        } else {
            UrlSecurityPolicy.normalizeHostLenient(host)
        }

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
