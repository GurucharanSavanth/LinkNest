package com.linknest.app

import android.app.LocaleManager
import android.os.LocaleList
import android.graphics.Bitmap
import android.view.View
import java.io.File
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import com.linknest.app.testing.visibleTextNode

@SdkSuppress(minSdkVersion = 33)
class RtlConfigurationTest {
    @Test fun arabicApplicationLocale_setsActualRootDirection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(LocaleManager::class.java)
        val original = manager.applicationLocales
        try {
            manager.applicationLocales = LocaleList.forLanguageTags("ar")
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    assertEquals(View.LAYOUT_DIRECTION_RTL, activity.resources.configuration.layoutDirection)
                    assertEquals(View.LAYOUT_DIRECTION_RTL, activity.window.decorView.layoutDirection)
                }
                val deadline = android.os.SystemClock.uptimeMillis() + 10_000
                while (instrumentation.uiAutomation.rootInActiveWindow
                        ?.visibleTextNode("Inbox") == null &&
                    android.os.SystemClock.uptimeMillis() < deadline) {
                    android.os.SystemClock.sleep(100)
                }
                org.junit.Assert.assertNotNull("Dashboard must finish rendering", instrumentation.uiAutomation.rootInActiveWindow?.visibleTextNode("Inbox"))
                val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                try {
                    File(context.getExternalFilesDir(null), "qa-rtl.png").outputStream().use {
                        check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                } finally { screenshot.recycle() }
            }
        } finally {
            manager.applicationLocales = original
        }
    }
}
