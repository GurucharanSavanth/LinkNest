package com.linknest.app

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.widget.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Test

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
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        try {
            instrumentation.runOnMainSync {
                host.startListening()
                listOf(LinkNestWidgetProvider::class.java, RecentLinksWidgetProvider::class.java,
                    CategoryLinksWidgetProvider::class.java, QuickAddWidgetProvider::class.java).forEach { provider ->
                    val id = host.allocateAppWidgetId().also { ids.add(it) }
                    assertTrue("Unable to bind ${provider.simpleName}",
                        manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, provider)))
                    host.createView(context, id, manager.getAppWidgetInfo(id))
                }
            }
            instrumentation.waitForIdleSync()
            listening.set(true)
            HomeWidgetRefresher(context).refreshAll()
            assertTrue("Not all widget providers delivered RemoteViews", latch.await(20, TimeUnit.SECONDS))
        } finally {
            instrumentation.runOnMainSync {
                ids.forEach(host::deleteAppWidgetId)
                host.stopListening()
            }
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
