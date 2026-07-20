package com.linknest.core.network

import java.net.IDN
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
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

    fun normalizeHostLenient(rawHost: String): Pair<String, Boolean> {
        val trimmed = rawHost.trim().trim('.')
        if (trimmed.isBlank()) {
            return rawHost.lowercase(Locale.US) to false
        }
        val ascii = runCatching {
            IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.US)
        }.getOrDefault(trimmed.lowercase(Locale.US))
        validateHostSyntaxLenient(ascii)
        return ascii to (ascii != trimmed.lowercase(Locale.US))
    }

    private fun validateHostSyntaxLenient(host: String) {
        if (host.isBlank()) return
        if (host.length > 253) return
        if (host.startsWith('[') && host.endsWith(']')) return
        if (IPV4_PATTERN.matches(host)) return
        if (!host.contains('.')) return
        host.split('.').forEach { label ->
            if (label.isBlank() || label.length > 63) return@forEach
        }
    }

    fun isHostAllowed(host: String): Boolean = runCatching {
        normalizeHost(host)
        true
    }.getOrDefault(false)

    /**
     * Trust-boundary guard for ANY outbound fetch (metadata, icons, health probes).
     * Throws if the target is not an http/https URL whose host — by literal form AND by
     * resolved IP — is a public destination. This is the only SSRF gate that must run
     * BEFORE a network call; the lenient normalizer deliberately stores private hosts but
     * must never fetch them. Residual: Jsoup/HttpURLConnection re-resolve on connect, so
     * DNS-rebinding TOCTOU is not fully closed here — the literal + first-resolution checks
     * block the common private-host and private-A-record cases.
     */
    fun requirePublicWebTarget(url: String) {
        val parsed = URI(url)
        val scheme = parsed.scheme?.lowercase(Locale.US)
        require(scheme == "http" || scheme == "https") {
            "Only HTTP(S) destinations can be fetched."
        }
        val host = parsed.host?.trim()?.trim('.')?.takeIf(String::isNotBlank)
            ?: error("URL host is invalid.")
        // Literal host / syntax / literal-IP private-range check.
        normalizeHost(host)
        // Resolved-address private-range check (public name pointing at a private IP).
        ensureResolvedAddressesPublic(host)
    }

    private fun ensureResolvedAddressesPublic(host: String) {
        // Unresolvable hosts fail closed on their own (no reachable target).
        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull() ?: return
        addresses.forEach { address ->
            // Behavioral flags are form-independent, so they catch loopback/link-local/site-local
            // regardless of whether getHostAddress returns "::1" or "0:0:0:0:0:0:0:1".
            requirePublicInetAddress(address)
            val literal = address.hostAddress?.substringBefore('%') ?: return@forEach
            when (address) {
                is Inet4Address -> ensurePublicIpv4(literal)  // adds CGNAT 100.64/10 and 0.x
                is Inet6Address -> ensurePublicIpv6(literal)  // adds ULA fc00::/7
            }
        }
    }

    private fun requirePublicInetAddress(address: InetAddress) {
        require(
            !address.isLoopbackAddress &&
                !address.isAnyLocalAddress &&
                !address.isLinkLocalAddress &&
                !address.isSiteLocalAddress,
        ) { "Local and private network hosts are blocked." }
    }

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
        require(parts.size >= 2) { "URL host is invalid." }
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
        // Parse the literal (no DNS for a numeric address) so any textual form — compressed "::1"
        // or expanded "0:0:0:0:0:0:0:1" — is judged by behavioral flags, not by string prefix.
        val parsed = runCatching { InetAddress.getByName(host.removeSurrounding("[", "]")) }.getOrNull()
        if (parsed != null) {
            requirePublicInetAddress(parsed)
            val canonical = parsed.hostAddress?.substringBefore('%')?.lowercase(Locale.US).orEmpty()
            // Unique-local (fc00::/7) is not covered by isSiteLocalAddress.
            require(!canonical.startsWith("fc") && !canonical.startsWith("fd")) {
                "Local and private network hosts are blocked."
            }
            return
        }
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
