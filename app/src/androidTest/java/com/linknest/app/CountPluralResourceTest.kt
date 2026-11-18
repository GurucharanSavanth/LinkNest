package com.linknest.app

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import com.linknest.feature.dashboard.R as DashboardR
import com.linknest.feature.settings.R as SettingsR

class CountPluralResourceTest {
    @Test fun englishCounts_andRestoreMessages_useSingularOnlyForOne() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.ENGLISH)
        }).resources
        for (count in listOf(0, 1, 2, 300)) {
            val websites = "$count ${if (count == 1) "website" else "websites"}"
            val categories = "$count ${if (count == 1) "category" else "categories"}"
            assertEquals(websites, resources.getQuantityString(DashboardR.plurals.dashboard_website_count, count, count))
            assertEquals(websites, resources.getQuantityString(SettingsR.plurals.settings_website_count, count, count))
            assertEquals(categories, resources.getQuantityString(SettingsR.plurals.settings_category_count, count, count))
            val outcomes = "$count ${if (count == 1) "recovery outcome" else "recovery outcomes"} confirmed"
            val protected = if (count == 1) "1 outcome remains protected because it is unknown."
                else "$count outcomes remain protected because they are unknown."
            assertEquals("$outcomes; $protected", resources.getString(SettingsR.string.settings_recovery_reconciliation_result,
                resources.getQuantityString(SettingsR.plurals.settings_recovery_confirmed_count, count, count),
                resources.getQuantityString(SettingsR.plurals.settings_recovery_protected_count, count, count)))
            assertEquals("Restored $websites in $categories.", resources.getString(
                SettingsR.string.settings_restored_counts, websites, categories))
        }
        assertEquals(
            "Replace 1 website with 1 website in 1 category. Portable images: 1; unavailable custom images: 0. Your previous library will be retained for recovery.",
            resources.getString(SettingsR.string.settings_replace_preview,
                resources.getQuantityString(SettingsR.plurals.settings_website_count, 1, 1),
                resources.getQuantityString(SettingsR.plurals.settings_website_count, 1, 1),
                resources.getQuantityString(SettingsR.plurals.settings_category_count, 1, 1), 1, 0),
        )
    }
}
