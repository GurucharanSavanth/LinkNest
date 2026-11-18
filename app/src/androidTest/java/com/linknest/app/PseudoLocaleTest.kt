package com.linknest.app

import android.app.LocaleManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import com.linknest.app.testing.visibleTextNode
import com.linknest.feature.settings.R as SettingsR

@SdkSuppress(minSdkVersion = 33)
class PseudoLocaleTest {
    @Test fun expandedSettingsResources_renderUnderActualPseudoLocale() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(LocaleManager::class.java)
        val original = manager.applicationLocales
        val english = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }).getString(SettingsR.string.settings_background_health_monitoring)
        try {
            manager.applicationLocales = LocaleList.forLanguageTags("en-XA")
            ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "linknest://settings".toUri()
            }).use { scenario ->
                var expanded = ""
                scenario.onActivity {
                    assertEquals("en-XA", it.resources.configuration.locales[0].toLanguageTag())
                    expanded = it.getString(SettingsR.string.settings_background_health_monitoring)
                    assertTrue("Pseudo resources must expand actual feature text", expanded.length > english.length)
                    assertNotEquals(english, expanded)
                }
                val deadline = SystemClock.uptimeMillis() + 10_000
                var scrolls = 0
                while (instrumentation.uiAutomation.rootInActiveWindow
                        ?.visibleTextNode(expanded) == null && SystemClock.uptimeMillis() < deadline) {
                    if (scrolls < 3) {
                        instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                            if (scrollForward(root)) scrolls++
                        }
                    }
                    SystemClock.sleep(100)
                }
                val control = instrumentation.uiAutomation.rootInActiveWindow?.visibleTextNode(expanded)
                control?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                instrumentation.waitForIdleSync()
                val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                try {
                    File(context.getExternalFilesDir(null), "qa-pseudo-settings.png").outputStream().use {
                        check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                } finally { screenshot.recycle() }
                assertNotNull("Expanded control must be reachable", control)
            }
        } finally { manager.applicationLocales = original }
    }

    private fun scrollForward(node: AccessibilityNodeInfo): Boolean {
        if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { if (scrollForward(it)) return true }
        }
        return false
    }
}
