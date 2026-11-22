package com.linknest.core.designsystem

import androidx.annotation.StringRes
import com.linknest.core.model.FollowUpStatus
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.WebsitePriority

@get:StringRes
val HealthStatus.labelResource: Int
    get() = when (this) {
        HealthStatus.UNKNOWN -> R.string.ui_health_unknown
        HealthStatus.OK -> R.string.ui_health_ok
        HealthStatus.LOGIN_REQUIRED -> R.string.ui_health_login_required
        HealthStatus.BLOCKED -> R.string.ui_health_blocked
        HealthStatus.REDIRECTED -> R.string.ui_health_redirected
        HealthStatus.DNS_FAILED -> R.string.ui_health_dns_failed
        HealthStatus.SSL_ISSUE -> R.string.ui_health_ssl_issue
        HealthStatus.DEAD -> R.string.ui_health_dead
        HealthStatus.TIMEOUT -> R.string.ui_health_timeout
    }

@get:StringRes
val WebsitePriority.labelResource: Int
    get() = when (this) {
        WebsitePriority.LOW -> R.string.ui_priority_low
        WebsitePriority.NORMAL -> R.string.ui_priority_normal
        WebsitePriority.HIGH -> R.string.ui_priority_high
        WebsitePriority.CRITICAL -> R.string.ui_priority_critical
    }

@get:StringRes
val FollowUpStatus.labelResource: Int
    get() = when (this) {
        FollowUpStatus.NONE -> R.string.ui_follow_up_none
        FollowUpStatus.REVIEW -> R.string.ui_follow_up_review
        FollowUpStatus.IN_PROGRESS -> R.string.ui_follow_up_in_progress
        FollowUpStatus.WAITING -> R.string.ui_follow_up_waiting
        FollowUpStatus.DONE -> R.string.ui_follow_up_done
    }
