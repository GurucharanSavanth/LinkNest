package com.linknest.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecentLinksWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context,
                    WidgetEntryPoint::class.java,
                )
                val links = entryPoint.getRecentWidgetLinksUseCase()(4)
                WidgetRemoteViewsFactory.updateWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetIds = appWidgetIds,
                ) {
                    WidgetRemoteViewsFactory.buildCollectionWidget(
                        context = context,
                        title = context.getString(com.linknest.app.R.string.widget_recent_title),
                        subtitle = context.getString(com.linknest.app.R.string.widget_recent_subtitle),
                        emptyText = context.getString(com.linknest.app.R.string.widget_recent_empty),
                        links = links,
                        titleDeepLink = "linknest://search?collection=recent",
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(ComponentName(context, javaClass))
        if (widgetIds.isNotEmpty()) {
            onUpdate(context, manager, widgetIds)
        }
    }
}
