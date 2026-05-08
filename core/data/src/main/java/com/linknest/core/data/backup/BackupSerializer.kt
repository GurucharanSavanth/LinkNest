package com.linknest.core.data.backup

import com.linknest.core.data.model.BackupCategory
import com.linknest.core.data.model.BackupIconCache
import com.linknest.core.data.model.BackupIntegrityEvent
import com.linknest.core.data.model.BackupPreferences
import com.linknest.core.data.model.BackupRecentQuery
import com.linknest.core.data.model.BackupSavedFilter
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.BackupTag
import com.linknest.core.data.model.BackupWebsite
import com.linknest.core.data.model.BackupWebsiteTag
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.IntegrityEventType
import com.linknest.core.model.WebsitePriority
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

data class BackupPackage(
    val payload: String,
    val fileName: String,
    val checksum: String,
)

@Singleton
class BackupSerializer @Inject constructor() {
    fun serialize(snapshot: BackupSnapshot): BackupPackage {
        val json = buildSnapshotJson(snapshot).toString(2)
        require(json.length <= MAX_CHARS) { "Backup payload is too large to export safely." }
        val compressed = gzip(json)
        val checksum = compressed.sha256()
        val envelope = buildEnvelope(snapshot, compressed, checksum).toString(2)
        return BackupPackage(
            payload = envelope,
            fileName = "linknest-backup-${snapshot.exportedAt}.json",
            checksum = checksum,
        )
    }

    fun deserialize(payload: String): BackupSnapshot {
        require(payload.length <= MAX_CHARS) { "Backup payload is too large." }
        val root = JSONObject(payload)
        val snapshotJson = if (root.optString("kind") == ENVELOPE_KIND) {
            parseEnvelope(root)
        } else {
            root
        }
        return parseSnapshot(snapshotJson)
    }

    private fun buildEnvelope(
        snapshot: BackupSnapshot,
        compressed: ByteArray,
        checksum: String,
    ): JSONObject = JSONObject().apply {
        put("kind", ENVELOPE_KIND)
        put("meta", JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("appVersion", snapshot.appVersion)
            put("schemaVersion", snapshot.schemaVersion)
            put("exportedAt", snapshot.exportedAt)
            put("checksum", checksum)
            put("compression", "GZIP")
        })
        put("data", Base64.getEncoder().encodeToString(compressed))
    }

    private fun parseEnvelope(root: JSONObject): JSONObject {
        val meta = root.getJSONObject("meta")
        val version = meta.getInt("version")
        require(version <= FORMAT_VERSION) { "Unsupported backup format version $version. Please update the app." }
        val compression = meta.optString("compression", "GZIP")
        require(compression == "GZIP") { "Unsupported backup compression: $compression" }
        val compressed = Base64.getDecoder().decode(root.getString("data"))
        val actualChecksum = compressed.sha256()
        val storedChecksum = meta.getString("checksum")
        require(storedChecksum == actualChecksum) { "Backup file is corrupted (checksum mismatch)." }
        val json = gunzip(compressed)
        require(json.length <= MAX_CHARS) { "Backup payload too large after decompression." }
        return JSONObject(json)
    }

    private fun parseSnapshot(root: JSONObject): BackupSnapshot {
        require(root.has("schemaVersion")) { "Invalid backup: missing schemaVersion." }
        require(root.has("categories")) { "Invalid backup: missing categories." }
        require(root.has("websites")) { "Invalid backup: missing websites." }
        require(root.has("tags")) { "Invalid backup: missing tags." }
        require(root.has("websiteTags")) { "Invalid backup: missing websiteTags." }
        require(root.has("mappings")) { "Invalid backup: missing mappings." }
        return BackupSnapshot(
            schemaVersion = root.optInt("schemaVersion", 1),
            exportedAt = root.getLong("exportedAt"),
            appVersion = root.optString("appVersion", "unknown"),
            categories = root.getJSONArray("categories").mapObjects { item ->
                BackupCategory(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    colorHex = item.getString("colorHex"),
                    iconType = IconType.valueOf(item.getString("iconType")),
                    iconValue = item.optString("iconValue").takeIf(String::isNotBlank),
                    sortOrder = item.getInt("sortOrder"),
                    isCollapsed = item.getBoolean("isCollapsed"),
                    isArchived = item.getBoolean("isArchived"),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            websites = root.getJSONArray("websites").mapObjects { item ->
                BackupWebsite(
                    id = item.getLong("id"),
                    categoryId = item.getLong("categoryId"),
                    title = item.getString("title"),
                    canonicalUrl = item.optString("canonicalUrl").takeIf(String::isNotBlank),
                    finalUrl = item.optString("finalUrl").takeIf(String::isNotBlank),
                    normalizedUrl = item.getString("normalizedUrl"),
                    domain = item.getString("domain"),
                    ogImageUrl = item.optString("ogImageUrl").takeIf(String::isNotBlank),
                    faviconUrl = item.optString("faviconUrl").takeIf(String::isNotBlank),
                    chosenIconSource = IconSource.valueOf(item.getString("chosenIconSource")),
                    customIconUri = item.optString("customIconUri").takeIf(String::isNotBlank),
                    emojiIcon = item.optString("emojiIcon").takeIf(String::isNotBlank),
                    tileSizeDp = item.optInt("tileSizeDp").takeIf { it > 0 },
                    sortOrder = item.getInt("sortOrder"),
                    isPinned = item.getBoolean("isPinned"),
                    openCount = item.getInt("openCount"),
                    lastOpenedAt = item.optLong("lastOpenedAt").takeIf { it > 0L },
                    lastCheckedAt = item.optLong("lastCheckedAt").takeIf { it > 0L },
                    healthStatus = runCatching { HealthStatus.valueOf(item.getString("healthStatus")) }.getOrDefault(HealthStatus.UNKNOWN),
                    note = item.optString("note").takeIf(String::isNotBlank),
                    reasonSaved = item.optString("reasonSaved").takeIf(String::isNotBlank),
                    priority = item.optString("priority").takeIf(String::isNotBlank)?.let(WebsitePriority::valueOf) ?: WebsitePriority.NORMAL,
                    followUpStatus = item.optString("followUpStatus").takeIf(String::isNotBlank)?.let(FollowUpStatus::valueOf) ?: FollowUpStatus.NONE,
                    revisitAt = item.optLong("revisitAt").takeIf { it > 0L },
                    sourceLabel = item.optString("sourceLabel").takeIf(String::isNotBlank),
                    customLabel = item.optString("customLabel").takeIf(String::isNotBlank),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            tags = root.getJSONArray("tags").mapObjects { item ->
                BackupTag(id = item.getLong("id"), name = item.getString("name"))
            },
            websiteTags = root.getJSONArray("websiteTags").mapObjects { item ->
                BackupWebsiteTag(websiteId = item.getLong("websiteId"), tagId = item.getLong("tagId"))
            },
            mappings = root.getJSONArray("mappings").mapObjects { item ->
                DomainCategoryMapping(
                    domain = item.getString("domain"),
                    categoryId = item.getLong("categoryId"),
                    usageCount = item.getInt("usageCount"),
                    lastUsedAt = item.getLong("lastUsedAt"),
                )
            },
            savedFilters = root.optJSONArray("savedFilters").mapObjectsOrEmpty { item ->
                BackupSavedFilter(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    specJson = item.getString("specJson"),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            events = root.optJSONArray("events").mapObjectsOrEmpty { item ->
                BackupIntegrityEvent(
                    id = item.getLong("id"),
                    type = runCatching { IntegrityEventType.valueOf(item.getString("type")) }.getOrDefault(IntegrityEventType.BACKUP_EXPORT),
                    title = item.getString("title"),
                    summary = item.getString("summary"),
                    successful = item.getBoolean("successful"),
                    createdAt = item.getLong("createdAt"),
                )
            },
            iconCache = root.optJSONArray("iconCache").mapObjectsOrEmpty { item ->
                BackupIconCache(
                    id = item.getLong("id"),
                    websiteId = item.getLong("websiteId"),
                    sourceUrl = item.optString("sourceUrl").takeIf(String::isNotBlank),
                    localUri = item.optString("localUri").takeIf(String::isNotBlank),
                    contentHash = item.optString("contentHash").takeIf(String::isNotBlank),
                    mimeType = item.optString("mimeType").takeIf(String::isNotBlank),
                    etag = item.optString("etag").takeIf(String::isNotBlank),
                    fetchedAt = item.getLong("fetchedAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            recentQueries = root.optJSONArray("recentQueries").mapObjectsOrEmpty { item ->
                BackupRecentQuery(
                    id = item.getLong("id"),
                    query = item.getString("query"),
                    useCount = item.getInt("useCount"),
                    lastUsedAt = item.getLong("lastUsedAt"),
                )
            },
            preferences = root.optJSONObject("preferences")?.let { prefs ->
                BackupPreferences(
                    layoutMode = prefs.getString("layoutMode"),
                    tileSizeDp = prefs.getInt("tileSizeDp"),
                    tileDensityMode = prefs.getString("tileDensityMode"),
                    backgroundHealthChecksEnabled = prefs.getBoolean("backgroundHealthChecksEnabled"),
                    encryptedBackupsEnabled = prefs.getBoolean("encryptedBackupsEnabled"),
                )
            },
        )
    }

    private fun buildSnapshotJson(snapshot: BackupSnapshot): JSONObject = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("exportedAt", snapshot.exportedAt)
        put("appVersion", snapshot.appVersion)
        put("categories", JSONArray().apply {
            snapshot.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("colorHex", c.colorHex)
                    put("iconType", c.iconType.name); put("iconValue", c.iconValue)
                    put("sortOrder", c.sortOrder); put("isCollapsed", c.isCollapsed)
                    put("isArchived", c.isArchived); put("createdAt", c.createdAt); put("updatedAt", c.updatedAt)
                })
            }
        })
        put("websites", JSONArray().apply {
            snapshot.websites.forEach { w ->
                put(JSONObject().apply {
                    put("id", w.id); put("categoryId", w.categoryId); put("title", w.title)
                    put("canonicalUrl", w.canonicalUrl); put("finalUrl", w.finalUrl)
                    put("normalizedUrl", w.normalizedUrl); put("domain", w.domain)
                    put("ogImageUrl", w.ogImageUrl); put("faviconUrl", w.faviconUrl)
                    put("chosenIconSource", w.chosenIconSource.name); put("customIconUri", w.customIconUri)
                    put("emojiIcon", w.emojiIcon); put("tileSizeDp", w.tileSizeDp)
                    put("sortOrder", w.sortOrder); put("isPinned", w.isPinned); put("openCount", w.openCount)
                    put("lastOpenedAt", w.lastOpenedAt); put("lastCheckedAt", w.lastCheckedAt)
                    put("healthStatus", w.healthStatus.name); put("note", w.note)
                    put("reasonSaved", w.reasonSaved); put("priority", w.priority.name)
                    put("followUpStatus", w.followUpStatus.name); put("revisitAt", w.revisitAt)
                    put("sourceLabel", w.sourceLabel); put("customLabel", w.customLabel)
                    put("createdAt", w.createdAt); put("updatedAt", w.updatedAt)
                })
            }
        })
        put("tags", JSONArray().apply {
            snapshot.tags.forEach { t -> put(JSONObject().apply { put("id", t.id); put("name", t.name) }) }
        })
        put("websiteTags", JSONArray().apply {
            snapshot.websiteTags.forEach { r -> put(JSONObject().apply { put("websiteId", r.websiteId); put("tagId", r.tagId) }) }
        })
        put("mappings", JSONArray().apply {
            snapshot.mappings.forEach { m ->
                put(JSONObject().apply {
                    put("domain", m.domain); put("categoryId", m.categoryId)
                    put("usageCount", m.usageCount); put("lastUsedAt", m.lastUsedAt)
                })
            }
        })
        put("savedFilters", JSONArray().apply {
            snapshot.savedFilters.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id); put("name", f.name); put("specJson", f.specJson)
                    put("createdAt", f.createdAt); put("updatedAt", f.updatedAt)
                })
            }
        })
        put("events", JSONArray().apply {
            snapshot.events.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("type", e.type.name); put("title", e.title)
                    put("summary", e.summary); put("successful", e.successful); put("createdAt", e.createdAt)
                })
            }
        })
        put("iconCache", JSONArray().apply {
            snapshot.iconCache.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id); put("websiteId", i.websiteId); put("sourceUrl", i.sourceUrl)
                    put("localUri", i.localUri); put("contentHash", i.contentHash)
                    put("mimeType", i.mimeType); put("etag", i.etag)
                    put("fetchedAt", i.fetchedAt); put("updatedAt", i.updatedAt)
                })
            }
        })
        put("recentQueries", JSONArray().apply {
            snapshot.recentQueries.forEach { q ->
                put(JSONObject().apply {
                    put("id", q.id); put("query", q.query)
                    put("useCount", q.useCount); put("lastUsedAt", q.lastUsedAt)
                })
            }
        })
        snapshot.preferences?.let { p ->
            put("preferences", JSONObject().apply {
                put("layoutMode", p.layoutMode); put("tileSizeDp", p.tileSizeDp)
                put("tileDensityMode", p.tileDensityMode)
                put("backgroundHealthChecksEnabled", p.backgroundHealthChecksEnabled)
                put("encryptedBackupsEnabled", p.encryptedBackupsEnabled)
            })
        }
    }

    private fun gzip(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).bufferedWriter(StandardCharsets.UTF_8).use { it.write(text) }
        return out.toByteArray()
    }

    private fun gunzip(compressed: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return "sha256:" + digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private inline fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> =
        buildList(length()) { repeat(length()) { i -> add(block(getJSONObject(i))) } }

    private inline fun <T> JSONArray?.mapObjectsOrEmpty(block: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else mapObjects(block)

    private companion object {
        const val ENVELOPE_KIND = "linknest.backup"
        const val FORMAT_VERSION = 2
        const val MAX_CHARS = 32 * 1024 * 1024
    }
}
