package com.linknest.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AddEditEntryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun openEditor_whenCategoriesAreAlreadyLoaded_doesNotCrash() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Inbox").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Add Website").performClick()
        compose.onNodeWithText("Website URL").assertExists()
        compose.onNodeWithText("Note or annotation").assertExists()
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.onNodeWithContentDescription("Add Website").assertExists()
    }
}
