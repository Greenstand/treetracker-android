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

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.CustomSnackbar
import org.junit.Assert.assertEquals
import org.junit.Test

class SnackBarUiTest : ComposeUiTest() {
    private var dismissals = 0

    @Test
    fun `snackbar shows the message and its action label`() {
        showSnackbar(actionLabel = ACTION)

        composeTestRule.onNodeWithText(MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText(ACTION).assertIsDisplayed()
    }

    @Test
    fun `snackbar without an action label shows no action`() {
        showSnackbar(actionLabel = null)

        composeTestRule.onNodeWithText(MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText(ACTION).assertDoesNotExist()
    }

    @Test
    fun `clicking the action invokes onDismiss`() {
        showSnackbar(actionLabel = ACTION)

        composeTestRule.onNodeWithText(ACTION).performClick()

        composeTestRule.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun `nothing is shown until a snackbar is requested`() {
        setContent {
            CustomSnackbar(snackbarHostState = remember { SnackbarHostState() })
        }

        composeTestRule.onNodeWithText(MESSAGE).assertDoesNotExist()
    }

    private fun showSnackbar(actionLabel: String?) {
        setContent {
            val hostState = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                hostState.showSnackbar(
                    message = MESSAGE,
                    actionLabel = actionLabel,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            CustomSnackbar(snackbarHostState = hostState, onDismiss = { dismissals++ })
        }
    }

    private companion object {
        const val MESSAGE = "Trees uploaded"
        const val ACTION = "OK"
    }
}