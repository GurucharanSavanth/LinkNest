package com.linknest.core.data.mapper

import com.linknest.core.database.entity.WebsiteEntryEntity

internal fun duplicateCounts(entries: List<WebsiteEntryEntity>): Map<Long, Int> {
    val normalized = entries.groupBy { it.normalizedUrl }
    val final = entries.filter { it.finalUrl != null }.groupBy { it.finalUrl }
    val canonical = entries.filter { it.canonicalUrl != null }.groupBy { it.canonicalUrl }
    val title = entries.groupBy { it.domain to it.title.lowercase(java.util.Locale.ROOT) }
    return entries.associate { entry ->
        val matches = buildSet {
            normalized[entry.normalizedUrl].orEmpty().forEach { add(it.id) }
            entry.finalUrl?.let { url ->
                final[url].orEmpty().forEach { add(it.id) }
                canonical[url].orEmpty().forEach { add(it.id) }
                normalized[url].orEmpty().forEach { add(it.id) }
            }
            title[entry.domain to entry.title.lowercase(java.util.Locale.ROOT)].orEmpty().forEach { add(it.id) }
            remove(entry.id)
        }
        entry.id to matches.size
    }
}
