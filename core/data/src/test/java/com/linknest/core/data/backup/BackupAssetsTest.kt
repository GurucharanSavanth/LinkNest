package com.linknest.core.data.backup

import com.linknest.core.data.model.BackupAsset
import com.linknest.core.data.model.BackupSnapshot
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class BackupAssetsTest {
    private val empty = BackupSnapshot(schemaVersion = 6, exportedAt = 1, categories = emptyList(),
        websites = emptyList(), tags = emptyList(), websiteTags = emptyList(), mappings = emptyList())
    private val bytes = byteArrayOf(1, 2, 3)
    private val asset = BackupAsset(bytes.assetChecksum(), "image/png", Base64.getEncoder().encodeToString(bytes))

    @Test fun legacySnapshotAndAdditiveMetadataRoundTrip() {
        val serializer = BackupSerializer()
        val restored = serializer.deserialize(serializer.serialize(empty.copy(omittedCustomImages = 2)).payload)
        assertEquals(empty.copy(omittedCustomImages = 2), restored)
        assertTrue(validateAssets(empty).isEmpty())
    }

    @Test fun traversalDuplicateChecksumAndUnreferencedAssetsFail() {
        for (assets in listOf(listOf(asset.copy(id = "../foreign")), listOf(asset, asset),
            listOf(asset.copy(id = "a".repeat(64))), listOf(asset))) {
            assertThrows(IllegalArgumentException::class.java) { validateAssets(empty.copy(assets = assets)) }
        }
    }

    @Test fun oversizedAndUnsupportedAssetsFailBeforePublication() {
        for (invalid in listOf(asset.copy(data = "A".repeat(MAX_ASSET_BYTES * 2)), asset.copy(mimeType = "image/svg+xml"))) {
            assertThrows(IllegalArgumentException::class.java) { validateAssets(empty.copy(assets = listOf(invalid))) }
        }
        assertThrows(IllegalArgumentException::class.java) { validateAssets(empty.copy(assets = List(MAX_ASSET_COUNT + 1) { asset })) }
        assertThrows(IllegalArgumentException::class.java) { validateAssets(empty.copy(mediaVersion = 2)) }
    }
}
