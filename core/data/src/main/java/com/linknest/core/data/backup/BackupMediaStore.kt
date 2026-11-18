package com.linknest.core.data.backup

import android.content.Context
import android.graphics.BitmapFactory
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.BackupAsset
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.storage.LinkNestStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URI
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class BackupMediaStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val directory get() = File(context.filesDir, "backup-media")

    suspend fun attach(snapshot: BackupSnapshot): BackupSnapshot = withContext(ioDispatcher) {
        val assets = LinkedHashMap<String, BackupAsset>()
        var omitted = 0
        var totalBytes = 0
        var totalPixels = 0L
        val websites = snapshot.websites.map { website ->
            val source = website.customIconUri
            if (source == null) return@map website
            val asset = try {
                val uri = URI(source)
                require(uri.scheme == "file")
                val file = File(uri).canonicalFile
                require(file.parentFile == directory.canonicalFile || file.parentFile == LinkNestStorage.iconCacheDirectory(context).canonicalFile)
                require(file.isFile && file.length() in 1..MAX_ASSET_BYTES.toLong())
                val bytes = file.inputStream().use { it.readBytesBounded(MAX_ASSET_BYTES) }
                val id = bytes.assetChecksum()
                if (id !in assets) {
                    val (mime, pixels) = inspect(bytes)
                    require(assets.size < MAX_ASSET_COUNT && totalBytes + bytes.size <= MAX_TOTAL_ASSET_BYTES && totalPixels + pixels <= MAX_TOTAL_PIXELS)
                    totalBytes += bytes.size
                    totalPixels += pixels
                    assets[id] = BackupAsset(id, mime, Base64.getEncoder().encodeToString(bytes))
                }
                assets.getValue(id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { omitted++; null }
            website.copy(customIconUri = null, customIconAssetId = asset?.id)
        }
        snapshot.copy(websites = websites, iconCache = emptyList(), assets = assets.values.toList(), omittedCustomImages = omitted)
    }

    /** Publishes only generated names; the caller removes unreferenced files after rollback/restart. */
    suspend fun publish(snapshot: BackupSnapshot): Map<String, String> = withContext(ioDispatcher) {
        val bytes = validateAssets(snapshot)
        var pixels = 0L
        bytes.forEach { (id, data) ->
            val (mime, count) = inspect(data)
            require(snapshot.assets.single { it.id == id }.mimeType == mime) { "Custom image type mismatch." }
            pixels += count
            require(pixels <= MAX_TOTAL_PIXELS) { "Custom images exceed the decoded pixel limit." }
        }
        if (bytes.isEmpty()) return@withContext emptyMap()
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create custom image directory." }
        val created = mutableListOf<File>()
        try {
            bytes.mapValues { (_, data) ->
                val file = File(directory, "asset-${UUID.randomUUID()}.image")
                created += file
                file.outputStream().use { output -> output.write(data); output.flush(); output.fd.sync() }
                check(file.length() == data.size.toLong()) { "Custom image publication failed." }
                file.toURI().toString()
            }
        } catch (failure: Exception) {
            created.forEach { it.delete() }
            throw failure
        }
    }

    suspend fun removeUnreferenced(referencedUris: Set<String>) = withContext(ioDispatcher) {
        val ownedDirectory = directory.canonicalFile
        val referencedFiles = referencedUris.mapNotNull { source ->
            runCatching {
                val uri = URI(source)
                if (uri.scheme != "file") return@runCatching null
                File(uri).canonicalFile.takeIf { it.parentFile == ownedDirectory }
            }.getOrNull()
        }.toSet()
        directory.listFiles().orEmpty().filter { it.name.matches(Regex("asset-[a-f0-9-]{36}\\.image")) }
            .filter { it.canonicalFile !in referencedFiles }.forEach { it.delete() }
    }

    private fun inspect(bytes: ByteArray): Pair<String, Long> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val pixels = bounds.outWidth.toLong() * bounds.outHeight
        require(bounds.outWidth in 1..2048 && bounds.outHeight in 1..2048 && pixels <= 4L * 1024 * 1024) { "Invalid custom image dimensions." }
        val mime = requireNotNull(bounds.outMimeType) { "Invalid custom image format." }
        require(mime in setOf("image/png", "image/jpeg", "image/webp")) { "Invalid custom image format." }
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "Custom image could not be decoded." }
        bitmap.recycle()
        return mime to pixels
    }

    private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) return output.toByteArray()
            require(read <= limit - output.size()) { "Custom image is too large." }
            output.write(buffer, 0, read)
        }
    }

    private companion object { const val MAX_TOTAL_PIXELS = 8L * 1024 * 1024 }
}
