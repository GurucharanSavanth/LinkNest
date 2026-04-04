package com.linknest.feature.search

enum class SearchSmartCollection(
    val routeValue: String,
    val label: String,
) {
    PINNED(routeValue = "pinned", label = "Pinned"),
    RECENT(routeValue = "recent", label = "Recent"),
    DUPLICATES(routeValue = "duplicates", label = "Duplicates"),
    NEEDS_ATTENTION(routeValue = "needs_attention", label = "Needs Attention"), ;

    companion object {
        fun fromRouteValue(value: String?): SearchSmartCollection? =
            entries.firstOrNull { it.routeValue.equals(value, ignoreCase = true) }
    }
}
