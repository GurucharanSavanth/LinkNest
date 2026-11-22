package com.linknest.app

import android.app.LocaleManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import com.linknest.app.testing.visibleTextNode
import com.linknest.core.model.IntegrityOverview
import com.linknest.feature.settings.IntegrityCenterScreen
import com.linknest.feature.settings.IntegrityCenterUiState
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

class LocalizedIntegrityValuesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun timestampsAndCountsUseTheScreenLocale() {
        val arabic = Locale.forLanguageTag("ar-EG-u-nu-arab")
        val timestamp = Calendar.getInstance().apply {
            clear()
            set(2024, Calendar.JANUARY, 3, 16, 37)
        }.timeInMillis
        val state = IntegrityCenterUiState(
            isLoading = false,
            overview = IntegrityOverview(lastHealthCheckAt = timestamp, brokenLinksCount = 1234),
        )
        compose.activityRule.scenario.onActivity { activity ->
            val configuration = Configuration(activity.resources.configuration).apply { setLocale(arabic) }
            val context = activity.createConfigurationContext(configuration)
            activity.setContent {
                CompositionLocalProvider(LocalConfiguration provides configuration, LocalContext provides context) {
                    IntegrityCenterScreen(
                        uiState = state,
                        snackbarHostState = remember { SnackbarHostState() },
                        onBack = {}, onRefresh = {}, onOpenHealthReport = {},
                    )
                }
            }
        }
        compose.onNodeWithText("٢٠٢٤", substring = true).assertExists()
        compose.onNodeWithText("٣٧", substring = true).assertExists()
        compose.onNodeWithText("١٬٢٣٤").assertExists()
    }
}
