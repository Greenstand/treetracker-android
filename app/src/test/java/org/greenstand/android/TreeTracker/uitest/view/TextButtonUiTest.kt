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

import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.TreeTrackerButton
import org.greenstand.android.TreeTracker.view.TreeTrackerButtonPreview
import org.greenstand.android.TreeTracker.view.TreeTrackerButtonShape
import org.junit.Assert.assertEquals
import org.junit.Test

class TextButtonUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `button renders its content`() {
        setContent {
            TreeTrackerButton(modifier = Modifier.size(120.dp), onClick = {}) {
                Text(LABEL)
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed()
    }

    @Test
    fun `enabled button invokes onClick`() {
        setContent {
            TreeTrackerButton(modifier = Modifier.size(120.dp), onClick = { clicks++ }) {
                Text(LABEL)
            }
        }

        composeTestRule.onNodeWithText(LABEL).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `every click invokes onClick again`() {
        setContent {
            TreeTrackerButton(modifier = Modifier.size(120.dp), onClick = { clicks++ }) {
                Text(LABEL)
            }
        }

        composeTestRule.onNodeWithText(LABEL).performClick()
        composeTestRule.onNodeWithText(LABEL).performClick()

        composeTestRule.runOnIdle { assertEquals(2, clicks) }
    }

    @Test
    fun `disabled button ignores clicks`() {
        setContent {
            TreeTrackerButton(
                modifier = Modifier.size(120.dp),
                isEnabled = false,
                onClick = { clicks++ },
            ) {
                Text(LABEL)
            }
        }

        composeTestRule.onNodeWithText(LABEL).assertIsDisplayed().performClick()

        composeTestRule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun `selected button still invokes onClick`() {
        setContent {
            TreeTrackerButton(
                modifier = Modifier.size(120.dp),
                isSelected = true,
                onClick = { clicks++ },
            ) {
                Text(LABEL)
            }
        }

        composeTestRule.onNodeWithText(LABEL).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `circle button invokes onClick`() {
        setContent {
            TreeTrackerButton(
                modifier = Modifier.size(120.dp),
                shape = TreeTrackerButtonShape.Circle,
                onClick = { clicks++ },
            ) {
                Text(LABEL)
            }
        }

        composeTestRule.onNodeWithText(LABEL).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `preview renders both sample buttons`() {
        setContent { TreeTrackerButtonPreview() }

        composeTestRule.onAllNodesWithText("Button 1").assertCountEquals(2)
    }

    private companion object {
        const val LABEL = "Press me"
    }
}