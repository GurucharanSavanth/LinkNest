package com.linknest.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetRemoteViewsFactory.updateWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetIds = appWidgetIds,
                ) { widgetId ->
                    WidgetRemoteViewsFactory.buildQuickAddWidget(
                        context = context,
                        minHeightDp = appWidgetManager.getAppWidgetOptions(widgetId)
                            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 72),
                        title = context.getString(com.linknest.app.R.string.widget_quick_add_title),
                        body = context.getString(com.linknest.app.R.string.widget_quick_add_body),
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("LinkNestWidget", "Widget update failed", failure)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

}
