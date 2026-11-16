package com.linknest.core.data.backup

import com.linknest.core.data.model.BackupSnapshot
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.WebsitePriority
import org.json.JSONObject

fun validateBackup(snapshot: BackupSnapshot) {
    require(snapshot.schemaVersion in 1..6) { "Unsupported database schema." }
    fun ids(values: List<Long>) {
        require(values.all { it > 0 } && values.distinct().size == values.size) { "Backup IDs must be positive and unique." }
    }
    ids(snapshot.categories.map { it.id }); ids(snapshot.websites.map { it.id })
    ids(snapshot.tags.map { it.id }); ids(snapshot.savedFilters.map { it.id })
    ids(snapshot.events.map { it.id }); ids(snapshot.iconCache.map { it.id })
    ids(snapshot.recentQueries.map { it.id })
    fun names(values: List<String>) {
        require(values.all { it.isNotBlank() } && values.distinct().size == values.size) { "Backup names must be nonempty and unique." }
    }
    names(snapshot.categories.map { it.name }); names(snapshot.tags.map { it.name })
    names(snapshot.savedFilters.map { it.name })
    require(snapshot.categories.all { Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?").matches(it.colorHex) }) {
        "Category colors must use #RRGGBB or #AARRGGBB."
    }
    val categories = snapshot.categories.map { it.id }.toSet()
    val websites = snapshot.websites.map { it.id }.toSet()
    val tags = snapshot.tags.map { it.id }.toSet()
    require(snapshot.websites.all { it.categoryId in categories }) { "Invalid website category." }
    require(snapshot.websiteTags.all { it.websiteId in websites && it.tagId in tags }) { "Invalid tag association." }
    require(snapshot.mappings.all { it.categoryId in categories }) { "Invalid category mapping." }
    require(snapshot.iconCache.all { it.websiteId in websites }) { "Invalid icon reference." }
    require(snapshot.websites.all { it.normalizedUrl.isNotBlank() && it.openCount >= 0 && (it.tileSizeDp == null || it.tileSizeDp in 48..512) }) { "Invalid website values." }
    snapshot.savedFilters.forEach { filter ->
        val spec = JSONObject(filter.specJson)
        fun validateEnums(key: String, allowed: Set<String>) {
            spec.optJSONArray(key)?.let { array ->
                repeat(array.length()) { require(array.getString(it) in allowed) { "Invalid saved filter $key." } }
            }
        }
        validateEnums("healthStatuses", HealthStatus.entries.map { it.name }.toSet())
        validateEnums("priorities", WebsitePriority.entries.map { it.name }.toSet())
        validateEnums("followUpStatuses", FollowUpStatus.entries.map { it.name }.toSet())
    }
}
