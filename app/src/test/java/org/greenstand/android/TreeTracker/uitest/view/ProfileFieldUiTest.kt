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
package org.greenstand.android.TreeTracker.uitest.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ProfileField
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFieldUiTest : ComposeUiTest() {
    private val changes = mutableListOf<String>()

    @Test
    fun `read only field shows the label and the value as plain text`() {
        setContent {
            ProfileField(label = LABEL, value = VALUE, editable = false, onValueChange = {})
        }

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithText(VALUE).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun `editable field shows the label and a text field holding the value`() {
        setContent {
            ProfileField(label = LABEL, value = VALUE, editable = true, onValueChange = {})
        }

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction() and hasText(VALUE)).assertIsDisplayed()
    }

    @Test
    fun `editing the field reports the new value`() {
        setContent {
            ProfileField(label = LABEL, value = "", editable = true, onValueChange = { changes += it })
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Grace")

        composeTestRule.runOnIdle { assertEquals("Grace", changes.last()) }
    }

    @Test
    fun `clearing the field reports an empty value`() {
        setContent {
            ProfileField(label = LABEL, value = VALUE, editable = true, onValueChange = { changes += it })
        }

        composeTestRule.onNode(hasSetTextAction()).performTextClearance()

        composeTestRule.runOnIdle { assertEquals("", changes.last()) }
    }

    private companion object {
        const val LABEL = "First name"
        const val VALUE = "Ada"
    }
}