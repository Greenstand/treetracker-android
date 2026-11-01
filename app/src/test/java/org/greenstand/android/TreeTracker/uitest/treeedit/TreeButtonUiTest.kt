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
package org.greenstand.android.TreeTracker.uitest.treeedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.treeedit.TreeButton
import org.greenstand.android.TreeTracker.treeedit.TreeList
import org.greenstand.android.TreeTracker.treeedit.TreeListAction
import org.greenstand.android.TreeTracker.treeedit.TreeListState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for a single [TreeButton] and the states of [TreeList] its grid moves through. */
class TreeButtonUiTest : ComposeUiTest() {
    private fun tree(
        id: Long,
        note: String = "",
        createdAt: String = "2026-05-06T07:08:09Z",
    ) = TreeEntity(
        uuid = "tree-$id",
        sessionId = 1,
        photoPath = null,
        photoUrl = null,
        note = note,
        latitude = 0.0,
        longitude = 0.0,
        createdAt = Instant.parse(createdAt),
    ).apply { this.id = id }

    @Test
    fun `tree button shows only the date part of the creation time`() {
        setContent { TreeButton(tree = tree(1, createdAt = "2025-12-31T23:59:59Z"), isSelected = false, onClick = {}) }

        composeTestRule.onNodeWithText("2025-12-31").assertIsDisplayed()
        composeTestRule.onNodeWithText("23:59", substring = true).assertDoesNotExist()
    }

    @Test
    fun `tree button hides a note that is only whitespace`() {
        setContent { TreeButton(tree = tree(1, note = "   "), isSelected = false, onClick = {}) }

        composeTestRule.onNodeWithText("   ").assertDoesNotExist()
        composeTestRule.onNodeWithText("2026-05-06").assertIsDisplayed()
    }

    @Test
    fun `tree button shows a real note`() {
        setContent { TreeButton(tree = tree(1, note = "Cedar near the school"), isSelected = false, onClick = {}) }

        composeTestRule.onNodeWithText("Cedar near the school").assertIsDisplayed()
    }

    @Test
    fun `clicking the tree button reports a click`() {
        var clicks = 0
        setContent { TreeButton(tree = tree(1, note = "Fig"), isSelected = true, onClick = { clicks++ }) }

        composeTestRule.onNodeWithText("Fig").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `trees planted on the same day are each listed`() {
        val trees = listOf(tree(1, createdAt = "2026-05-06T07:00:00Z"), tree(2, createdAt = "2026-05-06T18:00:00Z"))
        setContent { TreeList(state = TreeListState(trees = trees, isLoading = false)) }

        composeTestRule.onAllNodesWithText("2026-05-06").assertCountEquals(2)
    }

    @Test
    fun `clicking a tree reports that tree and not its neighbour`() {
        val actions = mutableListOf<TreeListAction>()
        val first = tree(1, note = "First")
        val second = tree(2, note = "Second")
        setContent {
            TreeList(state = TreeListState(trees = listOf(first, second), isLoading = false), onHandleAction = { actions += it })
        }

        composeTestRule.onNodeWithText("Second").performClick()

        assertEquals(listOf(TreeListAction.SelectTree(second)), actions)
    }

    @Test
    fun `forward arrow starts working once a tree gets selected`() {
        val openedTreeIds = mutableListOf<Long>()
        val first = tree(7, note = "First")
        var state by mutableStateOf(TreeListState(trees = listOf(first), isLoading = false))
        setContent { TreeList(state = state, onNavigateToDetail = { openedTreeIds += it }) }
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.runOnIdle { assertEquals(emptyList<Long>(), openedTreeIds) }

        state = state.copy(selectedTree = first)
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(7L), openedTreeIds) }
    }

    @Test
    fun `finishing loading with no trees swaps the grid for the no trees message`() {
        var state by mutableStateOf(TreeListState(isLoading = true))
        setContent { TreeList(state = state) }
        composeTestRule.onNodeWithText(string(R.string.tree_list_empty)).assertDoesNotExist()

        state = TreeListState(isLoading = false)

        composeTestRule.onNodeWithText(string(R.string.tree_list_empty)).assertIsDisplayed()
    }
}