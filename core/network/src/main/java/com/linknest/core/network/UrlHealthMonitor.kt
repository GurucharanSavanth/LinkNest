package com.linknest.core.network

import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.model.HealthStatus
import javax.net.ssl.SSLException
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class UrlHealthMonitor @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun probe(url: String): HealthStatus = withContext(ioDispatcher) {
        try {
            val parsed = URI(url)
            require(parsed.scheme?.lowercase() == "https") { "Only secure HTTPS health checks are allowed." }
            // Same SSRF guard as metadata/icon fetch: reject private/loopback targets by literal
            // host AND resolved IP before opening the connection (health checks run on a schedule
            // over user-supplied URLs).
            UrlSecurityPolicy.requirePublicWebTarget(url)

            var response = PublicWebClient.fetch(url, 0, method = "HEAD", redirects = false)
            if (response.code == 405 || response.code == 501) {
                response = PublicWebClient.fetch(url, 0, redirects = false)
            }
            when (response.code) {
                in 200..299 -> HealthStatus.OK
                401, 407 -> HealthStatus.LOGIN_REQUIRED
                403, 451 -> HealthStatus.BLOCKED
                in 300..399 -> HealthStatus.REDIRECTED
                408 -> HealthStatus.TIMEOUT
                in 400..599 -> HealthStatus.DEAD
                else -> HealthStatus.UNKNOWN
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            when (throwable) {
                is java.net.SocketTimeoutException -> HealthStatus.TIMEOUT
                is java.net.UnknownHostException -> HealthStatus.DNS_FAILED
                is SSLException -> HealthStatus.SSL_ISSUE
                else -> HealthStatus.UNKNOWN
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000
        const val USER_AGENT =
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36 LinkNest/1.0"
    }
}
