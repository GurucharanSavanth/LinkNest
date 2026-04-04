package com.linknest.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetRemoteViewsFactory.updateWidgets(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
        ) {
            WidgetRemoteViewsFactory.buildQuickAddWidget(
                context = context,
                title = context.getString(com.linknest.app.R.string.widget_quick_add_title),
                body = context.getString(com.linknest.app.R.string.widget_quick_add_body),
            )
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
