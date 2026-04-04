package com.linknest.core.model

enum class LayoutMode {
    LIST,
    GRID,
}

enum class TileDensityMode {
    COMPACT,
    COMFORTABLE,
    ADAPTIVE,
}

enum class IconType {
    AUTO,
    EMOJI,
    CUSTOM_IMAGE,
}

enum class IconSource {
    OG_IMAGE,
    REL_ICON,
    APPLE_TOUCH_ICON,
    FAVICON_FALLBACK,
    GENERATED,
    CUSTOM,
    EMOJI,
}

enum class HealthStatus {
    UNKNOWN,
    OK,
    LOGIN_REQUIRED,
    BLOCKED,
    REDIRECTED,
    DNS_FAILED,
    SSL_ISSUE,
    DEAD,
    TIMEOUT,
}

enum class WebsitePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
}

enum class FollowUpStatus {
    NONE,
    REVIEW,
    IN_PROGRESS,
    WAITING,
    DONE,
}

enum class DuplicateMatchType {
    EXACT_URL,
    NORMALIZED_URL,
    REDIRECTED_URL,
    EFFECTIVE_DESTINATION,
    TITLE_DOMAIN,
}

enum class DuplicateDecision {
    KEEP_BOTH,
    CANCEL_SAVE,
    REPLACE_EXISTING,
    MERGE_METADATA,
    MOVE_EXISTING,
}

enum class IntegrityEventType {
    HEALTH_SCAN,
    BACKUP_EXPORT,
    RESTORE_IMPORT,
    DUPLICATE_SCAN,
    CACHE_MAINTENANCE,
}

enum class SearchSuggestionType {
    RECENT_QUERY,
    SAVED_SEARCH,
    WEBSITE_TITLE,
    DOMAIN,
    CATEGORY,
    TAG,
    NOTE,
    HEALTH,
    FLAG,
}
