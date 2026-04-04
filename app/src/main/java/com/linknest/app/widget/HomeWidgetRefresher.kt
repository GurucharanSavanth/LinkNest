package com.linknest.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun refreshAll() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        refreshProvider(appWidgetManager, LinkNestWidgetProvider())
        refreshProvider(appWidgetManager, RecentLinksWidgetProvider())
        refreshProvider(appWidgetManager, CategoryLinksWidgetProvider())
        refreshProvider(appWidgetManager, QuickAddWidgetProvider())
    }

    private fun refreshProvider(
        appWidgetManager: AppWidgetManager,
        provider: AppWidgetProvider,
    ) {
        val componentName = ComponentName(context, provider.javaClass)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isNotEmpty()) {
            provider.onUpdate(context, appWidgetManager, widgetIds)
        }
    }
}
