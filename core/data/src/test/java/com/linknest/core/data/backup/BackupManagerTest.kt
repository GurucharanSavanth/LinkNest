package com.linknest.core.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupManagerTest {
    private val manager = BackupManager(
        appContext = null,
        ioDispatcher = Dispatchers.Unconfined,
        backupCryptoManager = BackupCryptoManager(),
    )

    @Test
    fun parse_v2Payload_preservesFullSnapshot() {
        val payload = v2Payload(snapshotJson())

        val parsed = manager.parse(payload)

        assertEquals(6, parsed.schemaVersion)
        assertEquals("0.1.0", parsed.appVersion)
        assertEquals(1, parsed.categories.size)
        assertEquals(true, parsed.categories.single().isArchived)
        assertEquals(1, parsed.websites.size)
        assertEquals(1, parsed.tags.size)
        assertEquals(1, parsed.websiteTags.size)
        assertEquals(1, parsed.mappings.size)
        assertEquals(1, parsed.savedFilters.size)
        assertEquals(1, parsed.events.size)
        assertEquals(1, parsed.iconCache.size)
        assertEquals(1, parsed.recentQueries.size)
        assertEquals("GRID", parsed.preferences?.layoutMode)
        assertEquals(false, parsed.preferences?.backgroundHealthChecksEnabled)
    }

    @Test
    fun parse_v2Payload_rejectsChecksumMismatch() {
        val payload = v2Payload(snapshotJson()).replace("sha256:", "sha256:bad")

        assertThrows(IllegalArgumentException::class.java) {
            manager.parse(payload)
        }
    }

    @Test
    fun parse_legacyPayload_stillImportsCurrentRequiredArrays() {
        val parsed = manager.parse(snapshotJson())

        assertEquals(1, parsed.websites.size)
        assertEquals("GRID", parsed.preferences?.layoutMode)
    }

    private fun v2Payload(snapshotJson: String): String {
        val compressed = gzip(snapshotJson)
        return JSONObject()
            .put("kind", "linknest.backup")
            .put(
                "meta",
                JSONObject()
                    .put("version", 2)
                    .put("appVersion", "0.1.0")
                    .put("schemaVersion", 6)
                    .put("exportedAt", 1234L)
                    .put("checksum", compressed.sha256())
                    .put("encryption", "none")
                    .put("compression", "GZIP"),
            )
            .put("data", Base64.getEncoder().encodeToString(compressed))
            .toString()
    }

    private fun snapshotJson(): String =
        """
        {
          "schemaVersion": 6,
          "exportedAt": 1234,
          "appVersion": "0.1.0",
          "categories": [
            {
              "id": 1,
              "name": "Archive",
              "colorHex": "#445566",
              "iconType": "EMOJI",
              "iconValue": "A",
              "sortOrder": 0,
              "isCollapsed": false,
              "isArchived": true,
              "createdAt": 10,
              "updatedAt": 20
            }
          ],
          "websites": [
            {
              "id": 2,
              "categoryId": 1,
              "title": "Example",
              "canonicalUrl": "https://example.com",
              "finalUrl": "https://example.com/",
              "normalizedUrl": "https://example.com/",
              "domain": "example.com",
              "ogImageUrl": "",
              "faviconUrl": "https://example.com/favicon.ico",
              "chosenIconSource": "REL_ICON",
              "customIconUri": "",
              "emojiIcon": "",
              "tileSizeDp": 144,
              "sortOrder": 0,
              "isPinned": true,
              "openCount": 4,
              "lastOpenedAt": 30,
              "lastCheckedAt": 40,
              "healthStatus": "OK",
              "note": "note",
              "reasonSaved": "reason",
              "priority": "HIGH",
              "followUpStatus": "REVIEW",
              "revisitAt": 50,
              "sourceLabel": "manual",
              "customLabel": "docs",
              "createdAt": 10,
              "updatedAt": 20
            }
          ],
          "tags": [
            { "id": 3, "name": "dev" }
          ],
          "websiteTags": [
            { "websiteId": 2, "tagId": 3 }
          ],
          "mappings": [
            { "domain": "example.com", "categoryId": 1, "usageCount": 2, "lastUsedAt": 60 }
          ],
          "savedFilters": [
            { "id": 4, "name": "Pinned", "specJson": "{}", "createdAt": 10, "updatedAt": 20 }
          ],
          "events": [
            {
              "id": 5,
              "type": "BACKUP_EXPORT",
              "title": "Export",
              "summary": "ok",
              "successful": true,
              "createdAt": 70
            }
          ],
          "iconCache": [
            {
              "id": 6,
              "websiteId": 2,
              "sourceUrl": "https://example.com/favicon.ico",
              "localUri": "file://icon.webp",
              "contentHash": "abc",
              "mimeType": "image/webp",
              "etag": "v1",
              "fetchedAt": 80,
              "updatedAt": 90
            }
          ],
          "recentQueries": [
            { "id": 7, "query": "example", "useCount": 3, "lastUsedAt": 100 }
          ],
          "preferences": {
            "layoutMode": "GRID",
            "tileSizeDp": 160,
            "tileDensityMode": "COMFORTABLE",
            "backgroundHealthChecksEnabled": false,
            "encryptedBackupsEnabled": true
          }
        }
        """.trimIndent()

    private fun gzip(plainText: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(plainText)
        }
        return output.toByteArray()
    }

    @Suppress("unused")
    private fun gunzip(compressed: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(StandardCharsets.UTF_8).use { reader ->
            reader.readText()
        }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return "sha256:" + digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
