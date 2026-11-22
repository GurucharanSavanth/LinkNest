package com.linknest.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test

/** Exercises saved UI state without saving, deleting, or replacing library records. */
class StateRestorationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun categoryEditDraft_survivesRecreation_withoutSaving() {
        waitForInbox()
        compose.onNodeWithText("Inbox").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithText("Rename or recolor").performClick()
        compose.onNodeWithText("Category name").performTextReplacement("Unsaved recreation draft")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Unsaved recreation draft").assertIsDisplayed()
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        compose.onNodeWithText("Edit Category").assertDoesNotExist()
        compose.onNodeWithText("Inbox").assertIsDisplayed()
    }

    @Test fun categoryCreateDraft_survivesRecreation_andCancelDoesNotSave() {
        waitForInbox()
        compose.onNodeWithContentDescription("Add Website").performClick()
        compose.onNodeWithText("Create category").performScrollTo().performClick()
        compose.onNodeWithText("Category name").performTextInput("Unsaved new category")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Unsaved new category").assertIsDisplayed()
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        compose.onNodeWithText("Create Category").assertDoesNotExist()
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.onNodeWithText("Unsaved new category").assertDoesNotExist()
    }

    private fun waitForInbox() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Inbox").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
