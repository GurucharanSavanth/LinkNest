package com.linknest.app.widget

import androidx.core.net.toUri
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import kotlin.math.roundToInt
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
        minHeightDp: Int = 120,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.app_widget_collection).apply {
        val density = context.resources.displayMetrics.density
        // Leave rounding slack at fractional densities without shrinking 48dp actions.
        val availableHeight = (minHeightDp * density).roundToInt() - 2 * (11 * density).roundToInt()
        val titleHeight = maxOf((48 * density).roundToInt(), textHeight(context, 15f, bold = true))
        val rowHeight = maxOf((48 * density).roundToInt(), textHeight(context, 13f) + 2 * (6 * density).roundToInt())
        val visibleRows = minOf(links.size.coerceAtLeast(1), ((availableHeight - titleHeight) / rowHeight).coerceIn(1, itemIds.size))
        val subtitleHeight = textHeight(context, 11f) + (2 * density).roundToInt() + (8 * density).roundToInt()
        val showSubtitle = subtitle.isNotBlank() && availableHeight >= titleHeight + visibleRows * rowHeight + subtitleHeight
        setTextViewText(R.id.widget_title, title)
        setTextViewText(R.id.widget_subtitle, subtitle)
        setViewVisibility(R.id.widget_subtitle, if (showSubtitle) View.VISIBLE else View.GONE)
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
            if (index >= visibleRows) {
                setViewVisibility(viewId, View.GONE)
            } else if (link == null) {
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
                        requestCode = (link.websiteId and 0x7FFFFFFF).toInt(),
                    ),
                )
            }
        }
    }

    fun buildQuickAddWidget(
        context: Context,
        title: String,
        body: String,
        minHeightDp: Int = 72,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.app_widget_quick_add).apply {
        val density = context.resources.displayMetrics.density
        val contentHeight = (minHeightDp * density).roundToInt() - 2 * (12 * density).roundToInt()
        val requiredHeight = textHeight(context, 15f, bold = true) + textHeight(context, 13f) + (8 * density).roundToInt()
        setViewVisibility(R.id.widget_body, if (contentHeight >= requiredHeight) View.VISIBLE else View.GONE)
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
        remoteViewsProvider: (Int) -> RemoteViews,
    ) {
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, remoteViewsProvider(widgetId))
        }
    }

    private fun textHeight(context: Context, sizeSp: Float, bold: Boolean = false): Int = Paint().apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, context.resources.displayMetrics)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }.fontMetricsInt.let { it.bottom - it.top }

    private fun deepLinkPendingIntent(
        context: Context,
        deepLink: String,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(context.packageName)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
