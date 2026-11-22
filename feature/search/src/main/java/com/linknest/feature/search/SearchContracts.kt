package com.linknest.feature.search

enum class SearchSmartCollection(
    val routeValue: String,
    @get:androidx.annotation.StringRes val labelResource: Int,
) {
    PINNED(routeValue = "pinned", labelResource = R.string.search_pinned),
    RECENT(routeValue = "recent", labelResource = R.string.search_recent),
    DUPLICATES(routeValue = "duplicates", labelResource = R.string.search_duplicates),
    NEEDS_ATTENTION(routeValue = "needs_attention", labelResource = R.string.search_needs_attention), ;

    companion object {
        fun fromRouteValue(value: String?): SearchSmartCollection? =
            entries.firstOrNull { it.routeValue.equals(value, ignoreCase = true) }
    }
}
