package com.linknest.core.data.storage

import android.content.Context
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.PersistedIconCache
import com.linknest.core.network.UrlSecurityPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class IconStorageManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun cacheIcon(
        sourceUrl: String?,
        fetchedAt: Long,
    ): PersistedIconCache? = withContext(ioDispatcher) {
        if (sourceUrl.isNullOrBlank()) return@withContext null
        val parsedUrl = runCatching { URL(sourceUrl) }.getOrNull() ?: return@withContext null
        if (parsedUrl.protocol?.lowercase(Locale.US) !in setOf("http", "https")) return@withContext null
        val host = parsedUrl.host?.takeIf(String::isNotBlank) ?: return@withContext null
        if (!UrlSecurityPolicy.isHostAllowed(host)) return@withContext null

        runCatching {
            val connection = (parsedUrl.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                val mimeType = connection.contentType?.substringBefore(';')?.trim()
                val extension = UrlSecurityPolicy.safeFileExtension(mimeType)
                val iconDir = LinkNestStorage.iconCacheDirectory(appContext).apply { mkdirs() }
                val tempFile = File.createTempFile("icon-", ".tmp", iconDir)
                val digest = MessageDigest.getInstance("SHA-256")
                val totalBytes = connection.inputStream.use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var written = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            written += read
                            if (written > MAX_ICON_BYTES) {
                                throw IllegalStateException("Icon payload exceeded allowed size.")
                            }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                        written
                    }
                }
                if (totalBytes <= 0L) {
                    tempFile.delete()
                    return@runCatching null
                }
                val hash = digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
                val iconFile = File(iconDir, "$hash.$extension")
                if (!iconFile.exists()) {
                    if (!tempFile.renameTo(iconFile)) {
                        tempFile.copyTo(iconFile, overwrite = true)
                    }
                }
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                PersistedIconCache(
                    sourceUrl = sourceUrl,
                    localUri = iconFile.toURI().toString(),
                    contentHash = hash,
                    mimeType = mimeType,
                    fetchedAt = fetchedAt,
                    updatedAt = fetchedAt,
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    suspend fun clearStaleIcons(
        maxAgeMillis: Long,
        currentTimeMillis: Long,
    ): Int = withContext(ioDispatcher) {
        val iconDir = LinkNestStorage.iconCacheDirectory(appContext)
        if (!iconDir.exists()) return@withContext 0
        iconDir.listFiles()
            .orEmpty()
            .filter { file -> file.isFile && currentTimeMillis - file.lastModified() > maxAgeMillis }
            .count { file -> file.delete() }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000
        const val MAX_ICON_BYTES = 512L * 1024L
        const val USER_AGENT =
            "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36 LinkNest/0.2"
    }
}
