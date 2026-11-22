package com.linknest.feature.dashboard

import android.content.res.Resources
import com.linknest.core.designsystem.labelResource
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.WebsiteListItem
import com.linknest.core.model.WebsitePriority

internal fun WebsiteListItem.dashboardMetadataSummary(resources: Resources): String? {
    if (reasonSaved.isNullOrBlank() && note.isNullOrBlank() && sourceLabel.isNullOrBlank() &&
        customLabel.isNullOrBlank() && priority == WebsitePriority.NORMAL && followUpStatus == FollowUpStatus.NONE
    ) return null

    val segments = buildSet {
        reasonSaved?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        note?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
        if (size < 2) sourceLabel?.trim()?.takeIf(String::isNotEmpty)?.let { add(resources.getString(R.string.dashboard_source, it)) }
        if (size < 2) customLabel?.trim()?.takeIf(String::isNotEmpty)?.let { add(resources.getString(R.string.dashboard_label, it)) }
        if (size < 2 && priority != WebsitePriority.NORMAL) {
            add(resources.getString(R.string.dashboard_priority, resources.getString(priority.labelResource)))
        }
        if (size < 2 && followUpStatus != FollowUpStatus.NONE) {
            add(resources.getString(followUpStatus.labelResource))
        }
    }.iterator()
    val first = segments.next()
    return if (segments.hasNext()) resources.getString(R.string.dashboard_metadata_pair, first, segments.next()) else first
}

internal fun WebsiteListItem.matchesDashboardQuery(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        domain.contains(query, ignoreCase = true) ||
        normalizedUrl.contains(query, ignoreCase = true) ||
        note.orEmpty().contains(query, ignoreCase = true) ||
        reasonSaved.orEmpty().contains(query, ignoreCase = true) ||
        sourceLabel.orEmpty().contains(query, ignoreCase = true) ||
        customLabel.orEmpty().contains(query, ignoreCase = true) ||
        tagNames.any { it.contains(query, ignoreCase = true) }
