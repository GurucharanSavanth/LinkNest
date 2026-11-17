package com.linknest.app.shortcut

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.linknest.app.MainActivity
import com.linknest.app.R
import com.linknest.feature.search.SearchSmartCollection
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun publishDynamicShortcuts() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        shortcutManager.dynamicShortcuts = listOf(
            buildShortcut(
                id = "shortcut_recent",
                shortLabel = context.getString(R.string.shortcut_recent_short),
                longLabel = context.getString(R.string.shortcut_recent_long),
                iconRes = android.R.drawable.ic_menu_recent_history,
                deepLink = "linknest://search?collection=${SearchSmartCollection.RECENT.routeValue}",
                rank = 0,
            ),
            buildShortcut(
                id = "shortcut_health",
                shortLabel = context.getString(R.string.shortcut_health_short),
                longLabel = context.getString(R.string.shortcut_health_long),
                iconRes = android.R.drawable.ic_popup_sync,
                deepLink = "linknest://health-report?autorun=true",
                rank = 1,
            ),
        )
    }

    fun reportActivation(intent: Intent): Boolean {
        val id = intent.getStringExtra(EXTRA_SHORTCUT_ID) ?: return false
        val destination = when (id) {
            "shortcut_recent" -> "linknest://search?collection=${SearchSmartCollection.RECENT.routeValue}"
            "shortcut_health" -> "linknest://health-report?autorun=true"
            "shortcut_add" -> "linknest://add"
            "shortcut_search" -> "linknest://search"
            else -> return false
        }
        if (intent.action != Intent.ACTION_VIEW || intent.dataString != destination) return false
        intent.removeExtra(EXTRA_SHORTCUT_ID)
        context.getSystemService(ShortcutManager::class.java)?.reportShortcutUsed(id)
        return true
    }

    private fun buildShortcut(
        id: String,
        shortLabel: String,
        longLabel: String,
        iconRes: Int,
        deepLink: String,
        rank: Int,
    ): ShortcutInfo = ShortcutInfo.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setRank(rank)
        .setIcon(Icon.createWithResource(context, iconRes))
        .setIntent(
            Intent(Intent.ACTION_VIEW, deepLink.toUri(), context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                `package` = context.packageName
                putExtra(EXTRA_SHORTCUT_ID, id)
            },
        )
        .build()

    companion object {
        const val EXTRA_SHORTCUT_ID = "com.linknest.app.extra.SHORTCUT_ID"
    }
}
