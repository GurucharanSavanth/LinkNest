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
                ) {
                    WidgetRemoteViewsFactory.buildCollectionWidget(
                        context = context,
                        title = snapshot.categoryName.ifBlank {
                            context.getString(com.linknest.app.R.string.widget_category_title)
                        },
                        subtitle = context.getString(com.linknest.app.R.string.widget_category_subtitle),
                        emptyText = context.getString(com.linknest.app.R.string.widget_category_empty),
                        links = snapshot.links,
                        titleDeepLink = snapshot.categoryId?.let { "linknest://category/$it" } ?: "linknest://dashboard",
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
