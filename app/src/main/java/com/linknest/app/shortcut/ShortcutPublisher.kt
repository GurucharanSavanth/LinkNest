package com.linknest.app.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import com.linknest.app.MainActivity
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
                shortLabel = "Recent",
                longLabel = "Open recent LinkNest results",
                iconRes = android.R.drawable.ic_menu_recent_history,
                deepLink = "linknest://search?collection=${SearchSmartCollection.RECENT.routeValue}",
                rank = 0,
            ),
            buildShortcut(
                id = "shortcut_health",
                shortLabel = "Run Health Check",
                longLabel = "Run a LinkNest health check",
                iconRes = android.R.drawable.ic_popup_sync,
                deepLink = "linknest://health-report?autorun=true",
                rank = 1,
            ),
        )
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
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                `package` = context.packageName
            },
        )
        .build()
}
