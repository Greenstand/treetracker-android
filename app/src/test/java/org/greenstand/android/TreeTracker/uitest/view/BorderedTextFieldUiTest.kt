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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.BorderedTextField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BorderedTextFieldUiTest : ComposeUiTest() {
    private val changes = mutableListOf<String>()

    @Test
    fun `text field shows its value`() {
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "Hello",
                onValueChange = {},
            )
        }

        composeTestRule.onNode(hasSetTextAction() and hasText("Hello")).assertExists()
    }

    @Test
    fun `text field shows the placeholder while empty`() {
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "",
                onValueChange = {},
                placeholder = { Text(PLACEHOLDER) },
            )
        }

        composeTestRule.onNodeWithText(PLACEHOLDER).assertExists()
    }

    @Test
    fun `typing reports the new value through onValueChange`() {
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "",
                onValueChange = { changes += it },
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("abc")

        composeTestRule.runOnIdle { assertEquals("abc", changes.last()) }
    }

    @Test
    fun `autofocus requests focus on the field`() {
        setContent {
            val focusRequester = remember { FocusRequester() }
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "",
                onValueChange = {},
                focusRequester = focusRequester,
                autofocusEnabled = true,
            )
        }

        composeTestRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun `without autofocus the field is not focused`() {
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "",
                onValueChange = {},
                focusRequester = remember { FocusRequester() },
                autofocusEnabled = false,
            )
        }

        composeTestRule.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun `focusing the field reports the focus change`() {
        val focusStates = mutableListOf<Boolean>()
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "",
                onValueChange = {},
                onFocusChanged = { focusStates += it.isFocused },
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performClick()

        composeTestRule.runOnIdle { assertTrue(focusStates.last()) }
    }

    @Test
    fun `the keyboard action runs the matching callback`() {
        var goActions = 0
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "",
                onValueChange = {},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { goActions++ }),
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        composeTestRule.runOnIdle { assertEquals(1, goActions) }
    }

    @Test
    fun `the placeholder is hidden once the field has a value`() {
        setContent {
            BorderedTextField(
                modifier = Modifier.height(80.dp),
                value = "Hello",
                onValueChange = {},
                placeholder = { Text(PLACEHOLDER) },
            )
        }

        composeTestRule.onNodeWithText(PLACEHOLDER).assertDoesNotExist()
    }

    private companion object {
        const val PLACEHOLDER = "Type here"
    }
}