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

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ActionBar
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionBarUiTest : ComposeUiTest() {
    @Test
    fun `action bar renders all three slots`() {
        setContent {
            ActionBar(
                leftAction = { Text(LEFT) },
                centerAction = { Text(CENTER) },
                rightAction = { Text(RIGHT) },
            )
        }

        composeTestRule.onNodeWithText(LEFT).assertIsDisplayed()
        composeTestRule.onNodeWithText(CENTER).assertIsDisplayed()
        composeTestRule.onNodeWithText(RIGHT).assertIsDisplayed()
    }

    @Test
    fun `action bar lays out slots from left to right`() {
        setContent {
            ActionBar(
                leftAction = { Text(LEFT) },
                centerAction = { Text(CENTER) },
                rightAction = { Text(RIGHT) },
            )
        }

        val left = composeTestRule.onNodeWithText(LEFT).getBoundsInRoot()
        val center = composeTestRule.onNodeWithText(CENTER).getBoundsInRoot()
        val right = composeTestRule.onNodeWithText(RIGHT).getBoundsInRoot()
        assertTrue(left.right <= center.left)
        assertTrue(center.right <= right.left)
    }

    @Test
    fun `action bar renders only the slots that are provided`() {
        setContent { ActionBar(centerAction = { Text(CENTER) }) }

        composeTestRule.onNodeWithText(CENTER).assertIsDisplayed()
        composeTestRule.onNodeWithText(LEFT).assertDoesNotExist()
        composeTestRule.onNodeWithText(RIGHT).assertDoesNotExist()
    }

    private companion object {
        const val LEFT = "left"
        const val CENTER = "center"
        const val RIGHT = "right"
    }
}