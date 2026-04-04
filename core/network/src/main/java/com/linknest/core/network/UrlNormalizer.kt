package com.linknest.core.network

import com.linknest.core.network.model.NormalizedUrl
import java.net.URI
import javax.inject.Inject

class UrlNormalizer @Inject constructor() {
    fun normalize(rawUrl: String): Result<NormalizedUrl> = runCatching {
        val candidate = rawUrl.trim()
        require(candidate.isNotBlank()) { "URL is required." }

        val withScheme = if (SCHEME_PATTERN.containsMatchIn(candidate)) {
            candidate
        } else {
            "https://$candidate"
        }

        val parsed = URI(withScheme)
        val originalScheme = parsed.scheme?.lowercase() ?: error("Missing URL scheme.")
        require(originalScheme == "http" || originalScheme == "https") {
            "Only HTTP and HTTPS URLs are allowed."
        }
        require(parsed.userInfo.isNullOrBlank()) { "Credentials in URLs are not supported." }
        require(parsed.fragment.isNullOrBlank()) { "Fragments are not supported in saved URLs." }
        require(parsed.rawAuthority?.contains('@') != true) { "Credentials in URLs are not supported." }

        val host = parsed.host?.trim()
            ?: error("URL must include a valid host.")
        val (asciiHost, isInternationalizedHost) = UrlSecurityPolicy.normalizeHost(host)
        val normalizedScheme = "https"

        val path = parsed.path?.ifBlank { "/" } ?: "/"
        val normalizedPort = parsed.port
            .takeUnless { it == 80 || it == 443 }
            ?: -1
        val normalized = URI(
            normalizedScheme,
            null,
            asciiHost,
            normalizedPort,
            path,
            parsed.query,
            null,
        ).toString()

        NormalizedUrl(
            rawInput = rawUrl,
            normalizedUrl = normalized,
            host = asciiHost,
            domain = asciiHost.removePrefix("www."),
            wasInsecureSchemeUpgraded = originalScheme == "http",
            isInternationalizedHost = isInternationalizedHost,
        )
    }

    private companion object {
        val SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z\\d+.-]*://")
    }
}
