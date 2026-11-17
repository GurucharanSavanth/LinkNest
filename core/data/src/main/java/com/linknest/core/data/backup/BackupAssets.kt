package com.linknest.core.data.backup

import com.linknest.core.data.model.BackupSnapshot
import java.security.MessageDigest
import java.util.Base64

internal const val MAX_ASSET_BYTES = 512 * 1024
internal const val MAX_TOTAL_ASSET_BYTES = 8 * 1024 * 1024
internal const val MAX_ASSET_COUNT = 128

internal fun validateAssets(snapshot: BackupSnapshot): Map<String, ByteArray> {
    require(snapshot.mediaVersion == 1) { "Unsupported custom image format." }
    require(snapshot.omittedCustomImages >= 0) { "Invalid omitted image count." }
    require(snapshot.assets.size <= MAX_ASSET_COUNT) { "Too many custom images." }
    var total = 0
    val assets = LinkedHashMap<String, ByteArray>()
    snapshot.assets.forEach { asset ->
        require(asset.id.matches(Regex("[a-f0-9]{64}")) && asset.id !in assets) { "Invalid or duplicate image ID." }
        require(asset.mimeType in setOf("image/png", "image/jpeg", "image/webp")) { "Unsupported custom image type." }
        require(asset.data.length <= ((MAX_ASSET_BYTES + 2) / 3) * 4) { "Custom image is too large." }
        val bytes = Base64.getDecoder().decode(asset.data)
        require(bytes.isNotEmpty() && bytes.size <= MAX_ASSET_BYTES) { "Custom image is empty or too large." }
        total += bytes.size
        require(total <= MAX_TOTAL_ASSET_BYTES) { "Custom images exceed the total byte limit." }
        require(bytes.assetChecksum() == asset.id) { "Custom image checksum mismatch." }
        assets[asset.id] = bytes
    }
    require(snapshot.websites.all { it.customIconAssetId == null || it.customIconAssetId in assets }) { "Missing custom image asset." }
    require(assets.keys.all { id -> snapshot.websites.any { it.customIconAssetId == id } }) { "Unreferenced custom image asset." }
    return assets
}

internal fun ByteArray.assetChecksum(): String = MessageDigest.getInstance("SHA-256")
    .digest(this).joinToString("") { "%02x".format(it) }
