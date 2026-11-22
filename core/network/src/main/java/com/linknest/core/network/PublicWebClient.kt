package com.linknest.core.network

import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PublicWebResponse(val url: String, val code: Int, val contentType: String?, val bytes: ByteArray)

object PublicWebClient {
    // Validate the exact addresses handed to the socket; a second DNS lookup cannot bypass the guard.
    private val client = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> = try {
                Dns.SYSTEM.lookup(hostname).onEach(UrlSecurityPolicy::requirePublicAddress)
            } catch (failure: IllegalArgumentException) {
                throw java.net.UnknownHostException("Destination is not public.").apply { initCause(failure) }
            }
        })
        .followRedirects(false).followSslRedirects(false)
        .callTimeout(12, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(url: String, maxBytes: Int, method: String = "GET", redirects: Boolean = true): PublicWebResponse = withContext(Dispatchers.IO) {
        var current = url
        repeat(6) {
            UrlSecurityPolicy.requirePublicWebTarget(current)
            val request = Request.Builder().url(current).method(method, null)
                .header("User-Agent", "LinkNest/0.1").build()
            val response = suspendCancellableCoroutine<Response> { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response) { _, value, _ -> value.close() }
                    }
                })
            }
            response.use { value ->
                if (redirects && value.code in 300..399) {
                    current = value.header("Location")?.let { value.request.url.resolve(it)?.toString() }
                        ?: error("Redirect destination is missing.")
                } else {
                    val bytes = value.body.byteStream().use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= maxBytes && maxBytes > 0) {
                            currentCoroutineContext().ensureActive()
                            val count = stream.read(buffer, 0, minOf(buffer.size, maxBytes - output.size() + 1))
                            if (count < 0) break
                            require(count <= maxBytes - output.size()) { "Response exceeds size limit." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    return@withContext PublicWebResponse(value.request.url.toString(), value.code, value.header("Content-Type"), bytes)
                }
            }
        }
        error("Too many redirects.")
    }
}
