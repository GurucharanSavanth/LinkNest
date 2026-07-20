package com.linknest.core.network

import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * SSRF trust-boundary guard. All cases use literal hosts so they resolve without a DNS
 * lookup — private literals throw at [UrlSecurityPolicy.normalizeHost] before any network
 * call, and the numeric public IP is parsed by InetAddress without a lookup.
 */
class UrlSecurityPolicyTest {

    @Test
    fun requirePublicWebTarget_rejectsNonHttpScheme() {
        assertThrows(Exception::class.java) {
            UrlSecurityPolicy.requirePublicWebTarget("ftp://example.com/")
        }
        assertThrows(Exception::class.java) {
            UrlSecurityPolicy.requirePublicWebTarget("file:///etc/passwd")
        }
    }

    @Test
    fun requirePublicWebTarget_rejectsLoopbackAndLocalhost() {
        listOf(
            "http://localhost/",
            "https://127.0.0.1/",
            "https://[::1]/",
        ).forEach { url ->
            assertThrows("expected reject: $url", Exception::class.java) {
                UrlSecurityPolicy.requirePublicWebTarget(url)
            }
        }
    }

    @Test
    fun requirePublicWebTarget_rejectsPrivateAndLinkLocalRanges() {
        listOf(
            "https://10.0.0.5/",
            "https://172.16.0.1/",
            "https://192.168.1.10/router",
            "https://169.254.169.254/latest/meta-data/", // cloud metadata
            "https://100.64.0.1/",                        // CGNAT
        ).forEach { url ->
            assertThrows("expected reject: $url", Exception::class.java) {
                UrlSecurityPolicy.requirePublicWebTarget(url)
            }
        }
    }

    @Test
    fun requirePublicWebTarget_rejectsIpv6LoopbackInAnyForm() {
        // Both the compressed and fully-expanded loopback literal must be rejected — the earlier
        // string-prefix check only matched "::1" and let "0:0:0:0:0:0:0:1" through.
        listOf(
            "https://[::1]/",
            "https://[0:0:0:0:0:0:0:1]/",
            "https://[fc00::1]/", // unique-local
        ).forEach { url ->
            assertThrows("expected reject: $url", Exception::class.java) {
                UrlSecurityPolicy.requirePublicWebTarget(url)
            }
        }
    }

    @Test
    fun requirePublicWebTarget_allowsPublicLiteralIp() {
        // Numeric literal → InetAddress parses without a DNS lookup, keeping the test hermetic.
        UrlSecurityPolicy.requirePublicWebTarget("https://8.8.8.8/")
    }
}
