package com.linknest.core.network

import java.net.IDN
import java.net.URI
import java.util.Locale

object UrlSecurityPolicy {
    fun normalizeHost(rawHost: String): Pair<String, Boolean> {
        val trimmed = rawHost.trim().trim('.')
        require(trimmed.isNotBlank()) { "URL must include a valid host." }
        val ascii = IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES)
            .lowercase(Locale.US)
        validateHostSyntax(ascii)
        ensurePublicTarget(ascii)
        return ascii to (ascii != trimmed.lowercase(Locale.US))
    }

    fun validateResolvedUrl(url: String) {
        val parsed = URI(url)
        val scheme = parsed.scheme?.lowercase(Locale.US)
        require(scheme == "https") { "Only secure HTTPS destinations are supported." }
        val host = parsed.host ?: error("Resolved URL host is invalid.")
        normalizeHost(host)
    }

    fun isHostAllowed(host: String): Boolean = runCatching {
        normalizeHost(host)
        true
    }.getOrDefault(false)

    fun sanitizeSearchQuery(raw: String): String =
        raw.trim()
            .replace(CONTROL_CHARACTERS, " ")
            .replace(MULTI_SPACE, " ")
            .take(MAX_QUERY_LENGTH)

    fun safeFileExtension(mimeType: String?): String {
        val normalized = mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.US)
            .orEmpty()
        return when (normalized) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/svg+xml" -> "svg"
            "image/x-icon",
            "image/vnd.microsoft.icon",
            -> "ico"
            else -> "bin"
        }
    }

    private fun validateHostSyntax(host: String) {
        require(host.length in 1..253) { "URL host is invalid." }
        if (host.startsWith('[') && host.endsWith(']')) {
            ensurePublicIpv6(host.removeSurrounding("[", "]"))
            return
        }
        if (IPV4_PATTERN.matches(host)) {
            ensurePublicIpv4(host)
            return
        }
        require(host.contains('.')) { "Local and private network hosts are blocked." }
        host.split('.').forEach { label ->
            require(label.isNotBlank() && label.length <= 63) { "URL host is invalid." }
            require(LABEL_PATTERN.matches(label)) { "URL host is invalid." }
            require(!label.startsWith('-') && !label.endsWith('-')) { "URL host is invalid." }
        }
    }

    private fun ensurePublicTarget(host: String) {
        if (host == "localhost" || host.endsWith(".local")) {
            error("Local and private network hosts are blocked.")
        }
        if (host.startsWith('[') && host.endsWith(']')) {
            ensurePublicIpv6(host.removeSurrounding("[", "]"))
        } else if (IPV4_PATTERN.matches(host)) {
            ensurePublicIpv4(host)
        }
    }

    private fun ensurePublicIpv4(host: String) {
        val parts = host.split('.').map(String::toInt)
        require(parts.all { it in 0..255 }) { "URL host is invalid." }
        val first = parts[0]
        val second = parts[1]
        val isPrivate = when {
            first == 10 -> true
            first == 127 -> true
            first == 169 && second == 254 -> true
            first == 172 && second in 16..31 -> true
            first == 192 && second == 168 -> true
            first == 100 && second in 64..127 -> true
            first == 0 -> true
            else -> false
        }
        require(!isPrivate) { "Local and private network hosts are blocked." }
    }

    private fun ensurePublicIpv6(host: String) {
        val normalized = host.lowercase(Locale.US)
        val isPrivate = normalized == "::1" ||
            normalized.startsWith("fc") ||
            normalized.startsWith("fd") ||
            normalized.startsWith("fe80") ||
            normalized == "::"
        require(!isPrivate) { "Local and private network hosts are blocked." }
    }

    private val IPV4_PATTERN = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    private val LABEL_PATTERN = Regex("^[a-z0-9-]+\$")
    private val CONTROL_CHARACTERS = Regex("[\\p{Cntrl}]")
    private val MULTI_SPACE = Regex("\\s+")
    private const val MAX_QUERY_LENGTH = 120
}
