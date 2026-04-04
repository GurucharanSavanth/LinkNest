package com.linknest.feature.dashboard

import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.model.WebsitePriority

internal fun WebsiteListItem.dashboardMetadataSummary(): String? {
    val segments = buildList {
        reasonSaved?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        note?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        sourceLabel?.trim()?.takeIf(String::isNotEmpty)?.let { add("Source: $it") }
        customLabel?.trim()?.takeIf(String::isNotEmpty)?.let { add("Label: $it") }
        if (priority != WebsitePriority.NORMAL) {
            add("${priority.displayName()} priority")
        }
        if (followUpStatus != FollowUpStatus.NONE) {
            add(followUpStatus.displayName())
        }
    }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(2)

    return segments.joinToString(" | ").takeIf(String::isNotBlank)
}

internal fun WebsiteListItem.matchesDashboardQuery(query: String): Boolean =
    title.lowercase().contains(query) ||
        domain.lowercase().contains(query) ||
        normalizedUrl.lowercase().contains(query) ||
        note.orEmpty().lowercase().contains(query) ||
        reasonSaved.orEmpty().lowercase().contains(query) ||
        sourceLabel.orEmpty().lowercase().contains(query) ||
        customLabel.orEmpty().lowercase().contains(query) ||
        tagNames.any { it.lowercase().contains(query) } ||
        priority.displayName().lowercase().contains(query) ||
        followUpStatus.displayName().lowercase().contains(query)

private fun WebsitePriority.displayName(): String = formatEnumLabel(name)

private fun FollowUpStatus.displayName(): String = formatEnumLabel(name)

private fun formatEnumLabel(raw: String): String =
    raw.lowercase()
        .split('_')
        .joinToString(" ") { token -> token.replaceFirstChar(Char::uppercaseChar) }
