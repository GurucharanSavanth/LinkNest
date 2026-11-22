package com.linknest.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.linknest.app.testing.ProfilingEntryPoint
import com.linknest.core.data.model.CategoryUpsertRequest
import com.linknest.core.data.model.PersistWebsiteRequest
import com.linknest.core.designsystem.labelResource
import com.linknest.core.model.HealthStatus
import com.linknest.core.model.IconSource
import com.linknest.core.model.IconType
import com.linknest.core.model.TileDensityMode
import com.linknest.core.model.UserPreferences
import com.linknest.feature.dashboard.R as DashboardR
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardTileLayoutTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun websitesKeepIndividualKeysWidthsAndLabelsInBothLayouts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val entry = EntryPointAccessors.fromApplication(context, ProfilingEntryPoint::class.java)
        val token = UUID.randomUUID().toString()
        val categoryName = "Tile layout $token"
        val domain = "$token.invalid"
        val category = runBlocking {
            entry.categories().upsertCategory(CategoryUpsertRequest(
                name = categoryName, colorHex = "#7C4DFF", iconType = IconType.AUTO, iconValue = null,
            ))
        }
        try {
            val records = runBlocking {
                val now = System.currentTimeMillis()
                (1..3).map { index ->
                    val title = "Tile $index"
                    val id = entry.websites().addWebsite(PersistWebsiteRequest(
                        categoryId = category.id, title = title, normalizedUrl = "https://$domain/$index",
                        domain = domain, canonicalUrl = null, finalUrl = null, ogImageUrl = null, faviconUrl = null,
                        chosenIconSource = IconSource.GENERATED, customIconUri = null, emojiIcon = null,
                        tileSizeDp = null, sortOrder = index, isPinned = false, healthStatus = HealthStatus.SSL_ISSUE,
                        reasonSaved = "Personal note", note = "Personal note", sourceLabel = "Fixture",
                        createdAt = now, updatedAt = now, lastCheckedAt = now,
                    ))
                    id to title
                }
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                val list = context.getString(DashboardR.string.dashboard_list_mode)
                val grid = context.getString(DashboardR.string.dashboard_grid_mode)
                compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription(list).fetchSemanticsNodes().isNotEmpty() }
                val original = if (compose.onNodeWithContentDescription(grid).fetchSemanticsNode()
                        .config[SemanticsProperties.Selected]) grid else list
                val container = compose.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
                fun selectMode(mode: String) {
                    container.performScrollToKey("dashboard-header")
                    val toggle = compose.onNodeWithContentDescription(mode)
                    toggle.performClick()
                    compose.waitUntil(5_000) { toggle.fetchSemanticsNode().config[SemanticsProperties.Selected] }
                }
                val source = context.getString(DashboardR.string.dashboard_source, "Fixture")
                val summary = context.getString(DashboardR.string.dashboard_metadata_pair, "Personal note", source)
                val health = context.getString(HealthStatus.SSL_ISSUE.labelResource)
                try {
                    var listWidth = 0f
                    for (mode in listOf(list, grid)) {
                        selectMode(mode)
                        val widths = records.map { (id, title) ->
                            container.performScrollToKey(id)
                            val description = context.getString(DashboardR.string.dashboard_tile_description, title, domain, health)
                            val tile = compose.onNodeWithContentDescription(description)
                            tile.assertIsDisplayed().assertHasClickAction().assert(hasText(summary))
                            tile.fetchSemanticsNode().boundsInRoot.width
                        }
                        widths.forEach { width -> assertEquals(widths.first(), width, 1f) }
                        if (mode == list) {
                            listWidth = widths.first()
                        } else {
                            val largestMinimum = TileDensityMode.entries.maxOf { UserPreferences(tileDensityMode = it).adaptiveGridMinSizeDp }
                            val twoColumnsFit = (2 * largestMinimum + 12) * context.resources.displayMetrics.density
                            if (listWidth >= twoColumnsFit) assertTrue("Expected multiple grid columns", widths.first() < listWidth)
                        }
                    }
                } finally {
                    selectMode(original)
                }
            }
        } finally {
            runBlocking {
                check(entry.categories().getCategoryById(category.id)?.name == categoryName)
                check(entry.websites().getAllWebsiteItems().filter { it.categoryId == category.id }.all { it.domain == domain })
                entry.categories().deleteCategory(category.id)
            }
        }
    }
}
