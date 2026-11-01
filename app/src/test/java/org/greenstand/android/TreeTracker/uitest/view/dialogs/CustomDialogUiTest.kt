/*
 * Copyright 2026 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.uitest.view.dialogs

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.dialogs.CustomDialog
import org.junit.Assert.assertEquals
import org.junit.Test
import org.greenstand.android.TreeTracker.uitest.TEXT_FIELD_DIALOG_QUALIFIERS
import org.robolectric.annotation.Config

@Config(qualifiers = TEXT_FIELD_DIALOG_QUALIFIERS)
class CustomDialogUiTest : ComposeUiTest() {
    private var positiveClicks = 0
    private var negativeClicks = 0
    private val inputs = mutableListOf<String>()

    @Test
    fun `dialog shows its title and text content`() {
        setContent { CustomDialog(title = TITLE, textContent = BODY) }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(BODY).assertIsDisplayed()
    }

    @Test
    fun `dialog renders custom content`() {
        setContent { CustomDialog(title = TITLE, content = { Text(CONTENT) }) }

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun `dialog without callbacks shows no buttons`() {
        setContent { CustomDialog(title = TITLE, textContent = BODY) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `dialog with only a positive callback shows only the approve button`() {
        setContent { CustomDialog(title = TITLE, onPositiveClick = {}) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `dialog with only a negative callback shows only the decline button`() {
        setContent { CustomDialog(title = TITLE, onNegativeClick = {}) }

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `clicking approve invokes only the positive callback`() {
        setContent {
            CustomDialog(
                title = TITLE,
                onPositiveClick = { positiveClicks++ },
                onNegativeClick = { negativeClicks++ },
            )
        }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, positiveClicks)
            assertEquals(0, negativeClicks)
        }
    }

    @Test
    fun `clicking decline invokes only the negative callback`() {
        setContent {
            CustomDialog(
                title = TITLE,
                onPositiveClick = { positiveClicks++ },
                onNegativeClick = { negativeClicks++ },
            )
        }

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, positiveClicks)
            assertEquals(1, negativeClicks)
        }
    }

    @Test
    fun `dialog without an input callback shows no text field`() {
        setContent { CustomDialog(title = TITLE, textContent = BODY) }

        composeTestRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun `input dialog shows the current input value`() {
        setContent {
            CustomDialog(
                title = TITLE,
                textInputValue = INPUT,
                onTextInputValueChange = {},
            )
        }

        composeTestRule.onNode(hasSetTextAction() and hasText(INPUT)).assertIsDisplayed()
    }

    @Test
    fun `typing in an input dialog reports the new value`() {
        setContent {
            CustomDialog(
                title = TITLE,
                onTextInputValueChange = { inputs += it },
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput(INPUT)

        composeTestRule.runOnIdle { assertEquals(INPUT, inputs.last()) }
    }

    @Test
    fun `dialog without an icon still shows its title`() {
        setContent { CustomDialog(dialogIcon = null, title = TITLE) }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    private companion object {
        const val TITLE = "Dialog title"
        const val BODY = "Dialog body"
        const val CONTENT = "Custom content"
        const val INPUT = "Typed value"
    }
}