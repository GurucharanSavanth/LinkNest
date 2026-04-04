package com.linknest.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.linknest.app.R
import com.linknest.core.model.WidgetLink

internal object WidgetRemoteViewsFactory {
    private val itemIds = listOf(
        R.id.widget_item_1,
        R.id.widget_item_2,
        R.id.widget_item_3,
        R.id.widget_item_4,
    )

    fun buildCollectionWidget(
        context: Context,
        title: String,
        subtitle: String,
        emptyText: String,
        links: List<WidgetLink>,
        titleDeepLink: String,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.app_widget_collection).apply {
        setTextViewText(R.id.widget_title, title)
        setTextViewText(R.id.widget_subtitle, subtitle)
        setViewVisibility(R.id.widget_subtitle, if (subtitle.isBlank()) View.GONE else View.VISIBLE)
        setOnClickPendingIntent(
            R.id.widget_title,
            deepLinkPendingIntent(
                context = context,
                deepLink = titleDeepLink,
                requestCode = title.hashCode(),
            ),
        )

        itemIds.forEachIndexed { index, viewId ->
            val link = links.getOrNull(index)
            if (link == null) {
                setViewVisibility(viewId, if (index == 0) View.VISIBLE else View.GONE)
                if (index == 0) {
                    setTextViewText(viewId, emptyText)
                    setOnClickPendingIntent(
                        viewId,
                        deepLinkPendingIntent(
                            context = context,
                            deepLink = "linknest://dashboard",
                            requestCode = 9000 + index,
                        ),
                    )
                }
            } else {
                setViewVisibility(viewId, View.VISIBLE)
                setTextViewText(viewId, "\u2022 ${link.title}")
                setOnClickPendingIntent(
                    viewId,
                    deepLinkPendingIntent(
                        context = context,
                        deepLink = "linknest://website/${link.websiteId}",
                        requestCode = link.websiteId.hashCode(),
                    ),
                )
            }
        }
    }

    fun buildQuickAddWidget(
        context: Context,
        title: String,
        body: String,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.app_widget_quick_add).apply {
        setTextViewText(R.id.widget_title, title)
        setTextViewText(R.id.widget_body, body)
        setOnClickPendingIntent(
            R.id.widget_root,
            deepLinkPendingIntent(
                context = context,
                deepLink = "linknest://add",
                requestCode = 4001,
            ),
        )
    }

    fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        remoteViewsProvider: () -> RemoteViews,
    ) {
        val views = remoteViewsProvider()
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun deepLinkPendingIntent(
        context: Context,
        deepLink: String,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(context.packageName)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
