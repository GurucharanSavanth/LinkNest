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

class CategoryLinksWidgetProvider : AppWidgetProvider() {
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
                val snapshot = entryPoint.getCategoryWidgetSnapshotUseCase()(limit = 4)
                WidgetRemoteViewsFactory.updateWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetIds = appWidgetIds,
                ) { widgetId ->
                    WidgetRemoteViewsFactory.buildCollectionWidget(
                        context = context,
                        minHeightDp = appWidgetManager.getAppWidgetOptions(widgetId)
                            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120),
                        title = snapshot.categoryName.ifBlank {
                            context.getString(com.linknest.app.R.string.widget_category_title)
                        },
                        subtitle = context.getString(com.linknest.app.R.string.widget_category_subtitle),
                        emptyText = context.getString(com.linknest.app.R.string.widget_category_empty),
                        links = snapshot.links,
                        titleDeepLink = snapshot.categoryId?.let { "linknest://category/$it" } ?: "linknest://dashboard",
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
