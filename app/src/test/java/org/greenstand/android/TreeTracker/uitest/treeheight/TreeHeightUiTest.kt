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
package org.greenstand.android.TreeTracker.uitest.treeheight

import androidx.compose.material.ButtonColors
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.treeheight.TreeHeight
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionState
import org.greenstand.android.TreeTracker.treeheight.treeHeightColorTag
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the stateless [TreeHeight] colour picker. */
class TreeHeightUiTest : ComposeUiTest() {
    private val defaultColors = TreeHeightSelectionState().colors
    private val selectedColors = mutableListOf<ButtonColors>()
    private var nextClicks = 0

    private fun render(state: TreeHeightSelectionState = TreeHeightSelectionState()) {
        setContent {
            TreeHeight(
                state = state,
                onColorSelected = { selectedColors += it },
                onNextClicked = { nextClicks++ },
            )
        }
    }

    @Test
    fun `title asks to select the tree height colour`() {
        render()

        composeTestRule
            .onNodeWithText(string(R.string.select_tree_height_colour).uppercase())
            .assertIsDisplayed()
    }

    @Test
    fun `one button is shown per colour`() {
        render()

        defaultColors.indices.forEach { index ->
            composeTestRule.onNodeWithTag(treeHeightColorTag(index)).assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(treeHeightColorTag(defaultColors.size)).assertDoesNotExist()
    }

    @Test
    fun `unselected colours use the small size`() {
        render()

        defaultColors.indices.forEach { index ->
            composeTestRule
                .onNodeWithTag(treeHeightColorTag(index))
                .assertWidthIsEqualTo(200.dp)
                .assertHeightIsEqualTo(62.dp)
        }
    }

    @Test
    fun `selected colour is enlarged and the others stay small`() {
        render(state = TreeHeightSelectionState(selectedColor = defaultColors[2]))

        composeTestRule
            .onNodeWithTag(treeHeightColorTag(2))
            .assertWidthIsEqualTo(350.dp)
            .assertHeightIsEqualTo(85.dp)
        composeTestRule
            .onNodeWithTag(treeHeightColorTag(0))
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(62.dp)
    }

    @Test
    fun `clicking a colour reports that colour`() {
        render()

        composeTestRule.onNodeWithTag(treeHeightColorTag(3)).performClick()

        assertEquals(listOf(defaultColors[3]), selectedColors)
    }

    @Test
    fun `forward arrow is inert until a colour is selected`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(0, nextClicks)
    }

    @Test
    fun `forward arrow reports next once a colour is selected`() {
        render(state = TreeHeightSelectionState(selectedColor = defaultColors[0]))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(1, nextClicks)
    }
}