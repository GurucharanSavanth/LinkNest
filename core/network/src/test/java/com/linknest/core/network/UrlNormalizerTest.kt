package com.linknest.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {
    private val normalizer = UrlNormalizer()

    @Test
    fun encodedComponents_preserveReservedDelimitersAndSignature() {
        val url = "https://example.com/a%2Fb?q=a%26b%3Dc%25&signature=abc%2Bdef#part%2Fone"
        assertEquals(url, normalizer.normalize(url).getOrThrow().normalizedUrl)
    }

    @Test fun credentialsAreRejectedAndExplicitHttpsPortIsPreserved() {
        assertTrue(normalizer.normalize("https://user:password@example.com/").isFailure)
        assertEquals("https://example.com:80/", normalizer.normalize("https://example.com:80/").getOrThrow().normalizedUrl)
    }

    @Test
    fun missingScheme_defaultsToHttps() {
        val result = normalizer.normalize("example.com/path?q=1").getOrThrow()

        assertEquals("https://example.com/path?q=1", result.normalizedUrl)
        assertFalse(result.hasUnsupportedScheme)
    }

    @Test
    fun http_isUpgradedToHttpsForSafeFetching() {
        val result = normalizer.normalize("http://example.com").getOrThrow()

        assertEquals("https://example.com/", result.normalizedUrl)
        assertTrue(result.wasInsecureSchemeUpgraded)
    }

    @Test
    fun customScheme_isStorableButMarkedUnsupportedForAutomation() {
        val result = normalizer.normalize("obsidian://open?vault=Links").getOrThrow()

        assertEquals("obsidian://open?vault=Links", result.normalizedUrl)
        assertEquals("open", result.host)
        assertTrue(result.hasUnsupportedScheme)
    }

    @Test
    fun unicodeDomain_isPunycodeNormalized() {
        val result = normalizer.normalize("https://bücher.example").getOrThrow()

        assertEquals("xn--bcher-kva.example", result.host)
        assertTrue(result.isInternationalizedHost)
    }

    @Test
    fun privateNetworkHttpTarget_isStoredByLenientNormalizer() {
        val result = normalizer.normalize("http://192.168.1.10").getOrThrow()

        assertEquals("https://192.168.1.10/", result.normalizedUrl)
    }

    @Test
    fun strictNormalizer_rejectsPrivateNetworkTarget() {
        assertTrue(normalizer.normalizeStrict("http://192.168.1.10").isFailure)
    }

    @Test
    fun unsafeScheme_isStoredButMarkedUnsupportedForAutomation() {
        val result = normalizer.normalize("javascript:alert(1)").getOrThrow()

        assertEquals("javascript:alert(1)", result.normalizedUrl)
        assertTrue(result.hasUnsupportedScheme)
    }

    @Test
    fun strictNormalizer_rejectsUnsafeScheme() {
        assertTrue(normalizer.normalizeStrict("javascript:alert(1)").isFailure)
    }
}
