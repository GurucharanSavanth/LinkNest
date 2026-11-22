package com.linknest.app

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 34)
@OptIn(ExperimentalTestApi::class)
class AccessibilityFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun mainScreens_passAccessibilityChecks() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Inbox").fetchSemanticsNodes().isNotEmpty()
        }
        compose.enableAccessibilityChecks()
        checkAccessibility()
        checkDashboardSemantics()
        compose.onNodeWithContentDescription("Add Website").performClick()
        checkAccessibility()
        compose.onNodeWithText("Create category").performScrollTo().performClick()
        checkAccessibility()
        dismissSheet()
        compose.onNodeWithText("Create Category").assertDoesNotExist()
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.onNodeWithContentDescription("Open advanced search").performClick()
        checkAccessibility()
        val filters = compose.onNodeWithContentDescription("Toggle filters")
        if (filters.fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)) {
            filters.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        } else {
            filters.performClick()
        }
        val pinnedCollection = compose.onNodeWithContentDescription("Pinned collection")
        val pinnedFlag = compose.onNodeWithText("Pinned")
        pinnedCollection.assertHasClickAction().assertIsNotSelected()
        pinnedFlag.assertHasClickAction().assertIsNotSelected()
        pinnedFlag.performSemanticsAction(SemanticsActions.OnClick) { it() }
        pinnedFlag.assertIsSelected()
        pinnedCollection.assertIsNotSelected()
        pinnedFlag.performSemanticsAction(SemanticsActions.OnClick) { it() }
        pinnedFlag.assertIsNotSelected()
        pinnedCollection.performSemanticsAction(SemanticsActions.OnClick) { it() }
        pinnedCollection.assertIsSelected()
        pinnedFlag.assertIsSelected().performSemanticsAction(SemanticsActions.OnClick) { it() }
        pinnedCollection.assertIsNotSelected()
        checkAccessibility()
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        checkAccessibility()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Background health monitoring"))
        compose.onNodeWithText("Background health monitoring")
            .assertIsToggleable()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(hasText("Periodic health checks in background with network constraints."))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Integrity Center"))
        compose.onNodeWithText("Integrity Center").performClick()
        checkAccessibility()
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Health Report"))
        compose.onNodeWithText("Health Report").performClick()
        checkAccessibility()
    }

    private fun checkDashboardSemantics() {
        val listMode = compose.onNodeWithContentDescription("List mode")
        val gridMode = compose.onNodeWithContentDescription("Grid mode")
        val initiallyGrid = gridMode.fetchSemanticsNode().config[SemanticsProperties.Selected]
        listMode.performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            listMode.fetchSemanticsNode().config[SemanticsProperties.Selected]
        }
        listMode.assertIsSelected()
        gridMode.assertIsNotSelected()
        gridMode.performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            gridMode.fetchSemanticsNode().config[SemanticsProperties.Selected]
        }
        gridMode.assertIsSelected()
        listMode.assertIsNotSelected()
        checkAccessibility()
        if (!initiallyGrid) {
            listMode.performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                listMode.fetchSemanticsNode().config[SemanticsProperties.Selected]
            }
        }

        val inbox = compose.onNodeWithText("Inbox")
        val initialState = inbox.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        val toggledState = if (initialState == "Collapsed") "Expanded" else "Collapsed"
        val expectedClickLabel = if (initialState == "Collapsed") "Expand category" else "Collapse category"
        inbox.assert(SemanticsMatcher("Has named category actions") { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.label == expectedClickLabel
        })
        inbox.performClick()
        waitForInboxState(toggledState)
        checkAccessibility()
        inbox.performClick()
        waitForInboxState(initialState)
        inbox.assert(SemanticsMatcher("Has named category long-click action") { node ->
            node.config.getOrNull(SemanticsActions.OnLongClick)?.label == "Show category actions"
        })
        inbox.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithText("Rename or recolor").assertIsDisplayed()
        checkAccessibility()
        dismissSheet()
        compose.onNodeWithText("Rename or recolor").assertDoesNotExist()
        inbox.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, initialState))
    }

    private fun dismissSheet() {
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss),
            useUnmergedTree = true,
        ).performSemanticsAction(SemanticsActions.Dismiss) { dismiss ->
            check(dismiss()) { "The bottom sheet rejected its dismiss action" }
        }
        compose.waitForIdle()
    }

    private fun waitForInboxState(state: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithText("Inbox").fetchSemanticsNode()
                .config[SemanticsProperties.StateDescription] == state
        }
        compose.onNodeWithText("Inbox")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))
    }

    private fun checkAccessibility() {
        compose.waitForIdle()
        compose.onAllNodes(isRoot()).fetchSemanticsNodes().indices.forEach { index ->
            compose.onAllNodes(isRoot())[index].tryPerformAccessibilityChecks()
        }
    }
}
