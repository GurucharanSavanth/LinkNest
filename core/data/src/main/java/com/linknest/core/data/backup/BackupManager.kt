package com.linknest.core.data.backup

import android.content.Context
import com.linknest.core.common.coroutine.IoDispatcher
import com.linknest.core.data.model.BackupArtifact
import com.linknest.core.data.model.BackupCategory
import com.linknest.core.data.model.BackupIntegrityEvent
import com.linknest.core.data.model.BackupSavedFilter
import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.data.model.BackupTag
import com.linknest.core.data.model.BackupWebsite
import com.linknest.core.data.model.BackupWebsiteTag
import com.linknest.core.data.model.DomainCategoryMapping
import com.linknest.core.data.storage.LinkNestStorage
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.IntegrityEventType
import com.linknest.core.model.WebsitePriority
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val backupCryptoManager: BackupCryptoManager,
) {
    suspend fun export(
        snapshot: BackupSnapshot,
        encrypted: Boolean,
    ): BackupArtifact = withContext(ioDispatcher) {
        val plainJson = buildJson(snapshot).toString(2)
        require(plainJson.length <= MAX_BACKUP_CHARS) { "Backup payload is too large to export safely." }
        val output = if (encrypted) backupCryptoManager.encrypt(plainJson) else plainJson
        val exportDir = LinkNestStorage.backupDirectory(appContext).apply { mkdirs() }
        val fileName = "linknest-backup-${snapshot.exportedAt}.${if (encrypted) "lnen" else "json"}"
        val file = File(exportDir, fileName)
        file.outputStream().bufferedWriter().use { writer -> writer.write(output) }
        BackupArtifact(
            fileName = fileName,
            filePath = file.absolutePath,
            json = output,
            isEncrypted = encrypted,
        )
    }

    fun parse(json: String): BackupSnapshot {
        require(json.length <= MAX_BACKUP_CHARS) { "Backup payload is too large to import safely." }
        val decryptedJson = backupCryptoManager.decryptIfNeeded(json)
        val root = JSONObject(decryptedJson)
        require(root.has("schemaVersion")) { "Backup schema version is missing." }
        require(root.has("categories")) { "Backup categories are missing." }
        require(root.has("websites")) { "Backup websites are missing." }
        require(root.has("tags")) { "Backup tags are missing." }
        require(root.has("websiteTags")) { "Backup tag links are missing." }
        require(root.has("mappings")) { "Backup domain mappings are missing." }
        return BackupSnapshot(
            schemaVersion = root.optInt("schemaVersion", 1),
            exportedAt = root.getLong("exportedAt"),
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
        )
    }

    private fun buildJson(snapshot: BackupSnapshot): JSONObject = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("exportedAt", snapshot.exportedAt)
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
        const val MAX_BACKUP_CHARS = 8 * 1024 * 1024
    }
}
