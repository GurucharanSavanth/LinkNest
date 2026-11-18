package com.linknest.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
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
                ) { widgetId ->
                    WidgetRemoteViewsFactory.buildCollectionWidget(
                        context = context,
                        minHeightDp = appWidgetManager.getAppWidgetOptions(widgetId)
                            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120),
                        title = context.getString(com.linknest.app.R.string.widget_recent_title),
                        subtitle = context.getString(com.linknest.app.R.string.widget_recent_subtitle),
                        emptyText = context.getString(com.linknest.app.R.string.widget_recent_empty),
                        links = links,
                        titleDeepLink = "linknest://search?collection=recent",
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
