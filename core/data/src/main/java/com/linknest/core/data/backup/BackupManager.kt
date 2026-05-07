package com.linknest.core.data.backup

import android.content.Context
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.BackupArtifact
import com.linknest.core.data.storage.LinkNestStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context?,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val backupCryptoManager: BackupCryptoManager,
) {
    suspend fun export(
        snapshot: BackupSnapshot,
        encrypted: Boolean,
    ): BackupArtifact = withContext(ioDispatcher) {
        val plainJson = buildJson(snapshot).toString(2)
        require(plainJson.length <= MAX_BACKUP_CHARS) { "Backup payload is too large to export safely." }
        val compressed = gzip(plainJson)
        val checksum = compressed.sha256()
        val envelope = buildEnvelope(snapshot, compressed, checksum, encrypted).toString(2)
        val output = if (encrypted) backupCryptoManager.encrypt(envelope) else envelope
        val ext = if (encrypted) "lnen" else "json"
        val fileName = "linknest-backup-${snapshot.exportedAt}.$ext"
        parse(output)
        val stagingFile = appContext?.let { ctx ->
            runCatching {
                val dir = LinkNestStorage.backupStagingDirectory(ctx).apply { mkdirs() }
                File(dir, "latest-backup.$ext").also { f -> f.writeText(output, Charsets.UTF_8) }
            }.getOrNull()
        }
        BackupArtifact(
            fileName = fileName,
            filePath = stagingFile?.absolutePath,
            json = output,
            isEncrypted = encrypted,
            checksum = checksum,
        )
    }

    fun latestStagedBackup(context: Context): File? {
        val dir = LinkNestStorage.backupStagingDirectory(context)
        return dir.listFiles()
            ?.filter { it.name.startsWith("latest-backup.") }
            ?.maxByOrNull { it.lastModified() }
            ?.takeIf { it.exists() && it.length() > 0 }
    }

    fun parse(json: String): BackupSnapshot {
        require(json.length <= MAX_BACKUP_CHARS) { "Backup payload is too large to import safely." }
        val decryptedJson = backupCryptoManager.decryptIfNeeded(json)
        require(decryptedJson.length <= MAX_BACKUP_CHARS) { "Backup payload is too large to import safely." }
        val root = JSONObject(decryptedJson)
        val snapshotRoot = if (root.optString("kind") == BACKUP_KIND) {
            parseEnvelope(root)
        } else {
            root
        }
        return parseSnapshot(snapshotRoot)
    }

    private fun parseSnapshot(root: JSONObject): BackupSnapshot {
        require(root.has("schemaVersion")) { "Backup schema version is missing." }
        require(root.has("categories")) { "Backup categories are missing." }
        require(root.has("websites")) { "Backup websites are missing." }
        require(root.has("tags")) { "Backup tags are missing." }
        require(root.has("websiteTags")) { "Backup tag links are missing." }
        require(root.has("mappings")) { "Backup domain mappings are missing." }
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
                    healthStatus = HealthStatus.valueOf(item.getString("healthStatus")),
                    note = item.optString("note").takeIf(String::isNotBlank),
                    reasonSaved = item.optString("reasonSaved").takeIf(String::isNotBlank),
                    priority = item.optString("priority").takeIf(String::isNotBlank)
                        ?.let(WebsitePriority::valueOf)
                        ?: WebsitePriority.NORMAL,
                    followUpStatus = item.optString("followUpStatus").takeIf(String::isNotBlank)
                        ?.let(FollowUpStatus::valueOf)
                        ?: FollowUpStatus.NONE,
                    revisitAt = item.optLong("revisitAt").takeIf { it > 0L },
                    sourceLabel = item.optString("sourceLabel").takeIf(String::isNotBlank),
                    customLabel = item.optString("customLabel").takeIf(String::isNotBlank),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            tags = root.getJSONArray("tags").mapObjects { item ->
                BackupTag(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                )
            },
            websiteTags = root.getJSONArray("websiteTags").mapObjects { item ->
                BackupWebsiteTag(
                    websiteId = item.getLong("websiteId"),
                    tagId = item.getLong("tagId"),
                )
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
                    type = IntegrityEventType.valueOf(item.getString("type")),
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
            preferences = root.optJSONObject("preferences")?.let { item ->
                BackupPreferences(
                    layoutMode = item.getString("layoutMode"),
                    tileSizeDp = item.getInt("tileSizeDp"),
                    tileDensityMode = item.getString("tileDensityMode"),
                    backgroundHealthChecksEnabled = item.getBoolean("backgroundHealthChecksEnabled"),
                    encryptedBackupsEnabled = item.getBoolean("encryptedBackupsEnabled"),
                )
            },
        )
    }

    private fun buildEnvelope(
        snapshot: BackupSnapshot,
        compressed: ByteArray,
        checksum: String,
        encrypted: Boolean,
    ): JSONObject = JSONObject().apply {
        put("kind", BACKUP_KIND)
        put(
            "meta",
            JSONObject().apply {
                put("version", BACKUP_FORMAT_VERSION)
                put("appVersion", snapshot.appVersion)
                put("schemaVersion", snapshot.schemaVersion)
                put("exportedAt", snapshot.exportedAt)
                put("checksum", checksum)
                put("encryption", if (encrypted) "AES-256-GCM" else "none")
                put("compression", "GZIP")
            },
        )
        put("data", Base64.getEncoder().encodeToString(compressed))
    }

    private fun parseEnvelope(root: JSONObject): JSONObject {
        val meta = root.getJSONObject("meta")
        val version = meta.getInt("version")
        require(version <= BACKUP_FORMAT_VERSION) { "Unsupported backup format version: $version" }
        require(meta.getString("compression") == "GZIP") { "Unsupported backup compression." }

        val compressed = Base64.getDecoder().decode(root.getString("data"))
        val checksum = compressed.sha256()
        require(meta.getString("checksum") == checksum) { "Backup checksum mismatch." }
        val plainJson = gunzip(compressed)
        require(plainJson.length <= MAX_BACKUP_CHARS) { "Backup payload is too large to import safely." }
        return JSONObject(plainJson)
    }

    private fun buildJson(snapshot: BackupSnapshot): JSONObject = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("exportedAt", snapshot.exportedAt)
        put("appVersion", snapshot.appVersion)
        put(
            "categories",
            JSONArray().apply {
                snapshot.categories.forEach { category ->
                    put(
                        JSONObject().apply {
                            put("id", category.id)
                            put("name", category.name)
                            put("colorHex", category.colorHex)
                            put("iconType", category.iconType.name)
                            put("iconValue", category.iconValue)
                            put("sortOrder", category.sortOrder)
                            put("isCollapsed", category.isCollapsed)
                            put("isArchived", category.isArchived)
                            put("createdAt", category.createdAt)
                            put("updatedAt", category.updatedAt)
                        },
                    )
                }
            },
        )
        put(
            "websites",
            JSONArray().apply {
                snapshot.websites.forEach { website ->
                    put(
                        JSONObject().apply {
                            put("id", website.id)
                            put("categoryId", website.categoryId)
                            put("title", website.title)
                            put("canonicalUrl", website.canonicalUrl)
                            put("finalUrl", website.finalUrl)
                            put("normalizedUrl", website.normalizedUrl)
                            put("domain", website.domain)
                            put("ogImageUrl", website.ogImageUrl)
                            put("faviconUrl", website.faviconUrl)
                            put("chosenIconSource", website.chosenIconSource.name)
                            put("customIconUri", website.customIconUri)
                            put("emojiIcon", website.emojiIcon)
                            put("tileSizeDp", website.tileSizeDp)
                            put("sortOrder", website.sortOrder)
                            put("isPinned", website.isPinned)
                            put("openCount", website.openCount)
                            put("lastOpenedAt", website.lastOpenedAt)
                            put("lastCheckedAt", website.lastCheckedAt)
                            put("healthStatus", website.healthStatus.name)
                            put("note", website.note)
                            put("reasonSaved", website.reasonSaved)
                            put("priority", website.priority.name)
                            put("followUpStatus", website.followUpStatus.name)
                            put("revisitAt", website.revisitAt)
                            put("sourceLabel", website.sourceLabel)
                            put("customLabel", website.customLabel)
                            put("createdAt", website.createdAt)
                            put("updatedAt", website.updatedAt)
                        },
                    )
                }
            },
        )
        put(
            "tags",
            JSONArray().apply {
                snapshot.tags.forEach { tag ->
                    put(
                        JSONObject().apply {
                            put("id", tag.id)
                            put("name", tag.name)
                        },
                    )
                }
            },
        )
        put(
            "websiteTags",
            JSONArray().apply {
                snapshot.websiteTags.forEach { ref ->
                    put(
                        JSONObject().apply {
                            put("websiteId", ref.websiteId)
                            put("tagId", ref.tagId)
                        },
                    )
                }
            },
        )
        put(
            "mappings",
            JSONArray().apply {
                snapshot.mappings.forEach { mapping ->
                    put(
                        JSONObject().apply {
                            put("domain", mapping.domain)
                            put("categoryId", mapping.categoryId)
                            put("usageCount", mapping.usageCount)
                            put("lastUsedAt", mapping.lastUsedAt)
                        },
                    )
                }
            },
        )
        put(
            "savedFilters",
            JSONArray().apply {
                snapshot.savedFilters.forEach { filter ->
                    put(
                        JSONObject().apply {
                            put("id", filter.id)
                            put("name", filter.name)
                            put("specJson", filter.specJson)
                            put("createdAt", filter.createdAt)
                            put("updatedAt", filter.updatedAt)
                        },
                    )
                }
            },
        )
        put(
            "events",
            JSONArray().apply {
                snapshot.events.forEach { event ->
                    put(
                        JSONObject().apply {
                            put("id", event.id)
                            put("type", event.type.name)
                            put("title", event.title)
                            put("summary", event.summary)
                            put("successful", event.successful)
                            put("createdAt", event.createdAt)
                        },
                    )
                }
            },
        )
        put(
            "iconCache",
            JSONArray().apply {
                snapshot.iconCache.forEach { icon ->
                    put(
                        JSONObject().apply {
                            put("id", icon.id)
                            put("websiteId", icon.websiteId)
                            put("sourceUrl", icon.sourceUrl)
                            put("localUri", icon.localUri)
                            put("contentHash", icon.contentHash)
                            put("mimeType", icon.mimeType)
                            put("etag", icon.etag)
                            put("fetchedAt", icon.fetchedAt)
                            put("updatedAt", icon.updatedAt)
                        },
                    )
                }
            },
        )
        put(
            "recentQueries",
            JSONArray().apply {
                snapshot.recentQueries.forEach { query ->
                    put(
                        JSONObject().apply {
                            put("id", query.id)
                            put("query", query.query)
                            put("useCount", query.useCount)
                            put("lastUsedAt", query.lastUsedAt)
                        },
                    )
                }
            },
        )
        snapshot.preferences?.let { preferences ->
            put(
                "preferences",
                JSONObject().apply {
                    put("layoutMode", preferences.layoutMode)
                    put("tileSizeDp", preferences.tileSizeDp)
                    put("tileDensityMode", preferences.tileDensityMode)
                    put("backgroundHealthChecksEnabled", preferences.backgroundHealthChecksEnabled)
                    put("encryptedBackupsEnabled", preferences.encryptedBackupsEnabled)
                },
            )
        }
    }

    private fun gzip(plainText: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(plainText)
        }
        return output.toByteArray()
    }

    private fun gunzip(compressed: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(StandardCharsets.UTF_8).use { reader ->
            reader.readText()
        }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return "sha256:" + digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private inline fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> =
        buildList(length()) {
            repeat(length()) { index ->
                add(block(getJSONObject(index)))
            }
        }

    private inline fun <T> JSONArray?.mapObjectsOrEmpty(block: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else mapObjects(block)

    private companion object {
        const val BACKUP_KIND = "linknest.backup"
        const val BACKUP_FORMAT_VERSION = 2
        const val MAX_BACKUP_CHARS = 32 * 1024 * 1024
    }
}
