package com.linknest.core.network

import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.model.HealthStatus
import javax.net.ssl.SSLException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
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
            require(parsed.scheme == "https") { "Only secure HTTPS health checks are allowed." }
            UrlSecurityPolicy.validateResolvedUrl(url)

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                requestMethod = "HEAD"
                setRequestProperty("User-Agent", USER_AGENT)
            }

            try {
                val statusCode = connection.responseCode
                when {
                    statusCode in 200..299 -> HealthStatus.OK
                    statusCode in setOf(401, 407) -> HealthStatus.LOGIN_REQUIRED
                    statusCode in setOf(403, 451) -> HealthStatus.BLOCKED
                    statusCode in 300..399 -> HealthStatus.REDIRECTED
                    statusCode == 408 -> HealthStatus.TIMEOUT
                    statusCode in 400..599 -> HealthStatus.DEAD
                    else -> HealthStatus.UNKNOWN
                }
            } finally {
                connection.disconnect()
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
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36 LinkNest/0.2"
    }
}
