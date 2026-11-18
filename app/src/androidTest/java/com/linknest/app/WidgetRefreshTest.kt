package com.linknest.app

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RemoteViews
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.widget.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = 29) // Shell permission adoption is unavailable on older APIs.
class WidgetRefreshTest {
    @Test fun resumeRefresh_updatesAllBoundWidgetTypesWithoutCrashing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = AppWidgetManager.getInstance(context)
        val updated = ConcurrentHashMap.newKeySet<Int>()
        val listening = AtomicBoolean(false)
        val latch = CountDownLatch(4)
        val host = object : AppWidgetHost(context, 0x4c4e5453) {
            override fun onCreateView(context: Context, id: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
                object : AppWidgetHostView(context) {
                    override fun updateAppWidget(views: RemoteViews?) {
                        super.updateAppWidget(views)
                        if (views != null && listening.get() && updated.add(id)) latch.countDown()
                    }
                }
        }
        val ids = mutableListOf<Int>()
        val views = mutableListOf<AppWidgetHostView>()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        try {
            scenario.onActivity { activity ->
                val root = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.LTGRAY)
                    val padding = (12 * resources.displayMetrics.density).toInt()
                    setPadding(padding, padding, padding, padding)
                }
                host.startListening()
                listOf(LinkNestWidgetProvider::class.java, RecentLinksWidgetProvider::class.java,
                    CategoryLinksWidgetProvider::class.java, QuickAddWidgetProvider::class.java).forEach { provider ->
                    val id = host.allocateAppWidgetId().also { ids.add(it) }
                    val component = ComponentName(context, provider)
                    val info = manager.installedProviders.single { it.provider == component }
                    val density = activity.resources.displayMetrics.density
                    val options = Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, (info.minWidth / density).toInt())
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, (info.minWidth / density).toInt())
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, (info.minHeight / density).toInt())
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (info.minHeight / density).toInt())
                    }
                    assertTrue("Unable to bind ${provider.simpleName}",
                        manager.bindAppWidgetIdIfAllowed(id, component, options))
                    val view = host.createView(activity, id, manager.getAppWidgetInfo(id))
                    views += view
                    root.addView(view, LinearLayout.LayoutParams(
                        info.minWidth + view.paddingLeft + view.paddingRight,
                        info.minHeight + view.paddingTop + view.paddingBottom,
                    ).apply { bottomMargin = (8 * activity.resources.displayMetrics.density).toInt() })
                }
                activity.setContentView(root)
            }
            instrumentation.waitForIdleSync()
            listening.set(true)
            HomeWidgetRefresher(context).refreshAll()
            assertTrue("Not all widget providers delivered RemoteViews", latch.await(20, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertTrue("Widget hosts must be attached and measured", views.all { it.isShown && it.width > 0 && it.height > 0 })
                assertTrue("Launcher-hosted widgets need their own readable background",
                    views.all { it.findViewById<android.view.View>(R.id.widget_root)?.background != null })
            }
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                File(context.getExternalFilesDir(null), "qa-widget-hosts.png").outputStream().use {
                    check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally { screenshot.recycle() }
        } finally {
            instrumentation.runOnMainSync {
                ids.forEach(host::deleteAppWidgetId)
                host.stopListening()
            }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
            scenario.close()
        }
    }
}
