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
    TIMEOUT;

    val displayName: String get() = when (this) {
        UNKNOWN -> "Unknown"
        OK -> "Good"
        LOGIN_REQUIRED -> "Login required"
        BLOCKED -> "Blocked"
        REDIRECTED -> "Redirected"
        DNS_FAILED -> "DNS failed"
        SSL_ISSUE -> "TLS issue"
        DEAD -> "Dead"
        TIMEOUT -> "Timeout"
    }
}

enum class WebsitePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL;

    val displayName: String get() = when (this) {
        LOW -> "Low"
        NORMAL -> "Normal"
        HIGH -> "High"
        CRITICAL -> "Critical"
    }
}

enum class FollowUpStatus {
    NONE,
    REVIEW,
    IN_PROGRESS,
    WAITING,
    DONE;

    val displayName: String get() = when (this) {
        NONE -> "None"
        REVIEW -> "Needs review"
        IN_PROGRESS -> "In progress"
        WAITING -> "Waiting"
        DONE -> "Done"
    }
}

enum class DuplicateMatchType {
    EXACT_URL,
    NORMALIZED_URL,
    REDIRECTED_URL,
    EFFECTIVE_DESTINATION,
    TITLE_DOMAIN;

    val displayName: String get() = when (this) {
        EXACT_URL -> "Exact URL"
        NORMALIZED_URL -> "Normalized URL"
        REDIRECTED_URL -> "Redirected target"
        EFFECTIVE_DESTINATION -> "Effective destination"
        TITLE_DOMAIN -> "Title and domain"
    }
}

enum class DuplicateDecision {
    KEEP_BOTH,
    CANCEL_SAVE,
    REPLACE_EXISTING,
    MERGE_METADATA,
    MOVE_EXISTING;

    val displayName: String get() = when (this) {
        KEEP_BOTH -> "Keep both"
        CANCEL_SAVE -> "Cancel save"
        REPLACE_EXISTING -> "Replace existing"
        MERGE_METADATA -> "Merge metadata"
        MOVE_EXISTING -> "Move existing"
    }
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
