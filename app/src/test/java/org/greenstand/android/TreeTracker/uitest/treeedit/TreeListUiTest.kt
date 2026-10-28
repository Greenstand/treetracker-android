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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeDetailRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.TreeListRoute
import org.greenstand.android.TreeTracker.treeedit.TreeList
import org.greenstand.android.TreeTracker.treeedit.TreeListAction
import org.greenstand.android.TreeTracker.treeedit.TreeListScreen
import org.greenstand.android.TreeTracker.treeedit.TreeListState
import org.greenstand.android.TreeTracker.treeedit.TreeListViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeListUiTest : ComposeUiTest() {
    private val actions = mutableListOf<TreeListAction>()
    private val openedTreeIds = mutableListOf<Long>()
    private var backClicks = 0

    private val filledState = TreeListState(trees = listOf(NOTED_TREE, PLAIN_TREE), isLoading = false)

    @Test
    fun `tree list shows the user name and the subtitle`() {
        setContent { TreeList(state = filledState, userName = USER_NAME) }

        composeTestRule.onNodeWithText(USER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tree_list_subtitle)).assertIsDisplayed()
    }

    @Test
    fun `loaded empty list shows the no trees message`() {
        setContent { TreeList(state = TreeListState(isLoading = false)) }

        composeTestRule.onNodeWithText(string(R.string.tree_list_empty)).assertIsDisplayed()
    }

    @Test
    fun `list that is still loading does not show the no trees message`() {
        setContent { TreeList(state = TreeListState(isLoading = true)) }

        composeTestRule.onNodeWithText(string(R.string.tree_list_empty)).assertDoesNotExist()
    }

    @Test
    fun `every tree shows its creation date`() {
        setContent { TreeList(state = filledState) }

        composeTestRule.onNodeWithText(NOTED_TREE.createdAt.toString().take(10)).assertIsDisplayed()
        composeTestRule.onNodeWithText(PLAIN_TREE.createdAt.toString().take(10)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tree_list_empty)).assertDoesNotExist()
    }

    @Test
    fun `a tree with a note shows the note`() {
        setContent { TreeList(state = filledState) }

        composeTestRule.onNodeWithText(NOTED_TREE.note).assertIsDisplayed()
    }

    @Test
    fun `clicking a tree selects it`() {
        setContent { TreeList(state = filledState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithText(PLAIN_TREE.createdAt.toString().take(10)).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(TreeListAction.SelectTree(PLAIN_TREE)), actions) }
    }

    @Test
    fun `forward is disabled while no tree is selected`() {
        setContent { TreeList(state = filledState, onNavigateToDetail = { openedTreeIds += it }) }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertTrue(openedTreeIds.isEmpty()) }
    }

    @Test
    fun `forward opens the selected tree`() {
        setContent {
            TreeList(
                state = filledState.copy(selectedTree = NOTED_TREE),
                onNavigateToDetail = { openedTreeIds += it },
            )
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(NOTED_TREE.id), openedTreeIds) }
    }

    @Test
    fun `back invokes onNavigateBack`() {
        setContent { TreeList(state = filledState, onNavigateBack = { backClicks++ }) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, backClicks) }
    }

    @Test
    fun `screen shows the trees of the user`() {
        setScreenContent(trees = listOf(NOTED_TREE, PLAIN_TREE))

        composeTestRule.onNodeWithText(USER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(NOTED_TREE.note).assertIsDisplayed()
    }

    @Test
    fun `screen shows the no trees message when the user has none`() {
        setScreenContent(trees = emptyList())

        composeTestRule.onNodeWithText(string(R.string.tree_list_empty)).assertIsDisplayed()
    }

    @Test
    fun `selecting a tree and going forward opens its detail`() {
        setScreenContent(trees = listOf(NOTED_TREE, PLAIN_TREE))

        composeTestRule.onNodeWithText(PLAIN_TREE.createdAt.toString().take(10)).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(TreeDetailRoute(treeId = PLAIN_TREE.id), backStack.last()) }
    }

    @Test
    fun `going forward without a selection does not navigate`() {
        setScreenContent(trees = listOf(NOTED_TREE, PLAIN_TREE))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(TreeListRoute(WALLET, USER_NAME), backStack.last()) }
    }

    @Test
    fun `back on the screen returns to the user select`() {
        setScreenContent(trees = listOf(NOTED_TREE))

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, TreeEditUserSelectRoute), backStack.toList())
        }
    }

    private fun setScreenContent(trees: List<TreeEntity>) {
        backStack.add(TreeEditUserSelectRoute)
        backStack.add(TreeListRoute(WALLET, USER_NAME))
        val dao = mockk<TreeTrackerDAO>()
        every { dao.getTreesByUserWallet(WALLET) } returns flowOf(trees)
        val viewModel = TreeListViewModel(WALLET, dao)
        setContent(inspectionMode = false) { TreeListScreen(WALLET, USER_NAME, viewModel) }
    }

    private companion object {
        const val WALLET = "ada@example.com"
        const val USER_NAME = "Ada Lovelace"

        val NOTED_TREE =
            TreeEntity(
                uuid = "tree-1",
                sessionId = 1,
                photoPath = null,
                photoUrl = null,
                note = "Mango by the river",
                latitude = 1.0,
                longitude = 2.0,
                createdAt = Instant.parse("2026-03-01T08:00:00Z"),
            ).apply { id = 11 }

        val PLAIN_TREE =
            TreeEntity(
                uuid = "tree-2",
                sessionId = 1,
                photoPath = null,
                photoUrl = null,
                note = "",
                latitude = 3.0,
                longitude = 4.0,
                createdAt = Instant.parse("2026-04-02T09:00:00Z"),
            ).apply { id = 12 }
    }
}