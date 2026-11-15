package com.linknest.core.data.storage

import android.content.Context
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.PersistedIconCache
import com.linknest.core.network.UrlSecurityPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
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
        var pendingFile: File? = null
        try {
            val response = com.linknest.core.network.PublicWebClient.fetch(sourceUrl, MAX_ICON_BYTES.toInt())
            if (response.code !in 200..299) return@withContext null
            val mimeType = response.contentType?.substringBefore(';')?.trim()
            if (mimeType?.startsWith("image/") != true) return@withContext null
            val extension = UrlSecurityPolicy.safeFileExtension(mimeType)
            val iconDir = LinkNestStorage.iconCacheDirectory(appContext).apply { mkdirs() }
            val tempFile = File.createTempFile("icon-", ".tmp", iconDir).also { pendingFile = it }
            val digest = MessageDigest.getInstance("SHA-256")
            val totalBytes = response.bytes.inputStream().use { input ->
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
                return@withContext null
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
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            pendingFile?.delete()
        }
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
        const val MAX_ICON_BYTES = 512L * 1024L

    }
}
