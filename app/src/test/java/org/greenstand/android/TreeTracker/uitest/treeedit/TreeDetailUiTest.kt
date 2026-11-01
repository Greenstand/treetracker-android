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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeDetailRoute
import org.greenstand.android.TreeTracker.navigation.TreeListRoute
import org.greenstand.android.TreeTracker.treeedit.TreeDetail
import org.greenstand.android.TreeTracker.treeedit.TreeDetailAction
import org.greenstand.android.TreeTracker.treeedit.TreeDetailScreen
import org.greenstand.android.TreeTracker.treeedit.TreeDetailState
import org.greenstand.android.TreeTracker.treeedit.TreeDetailViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.viewmodel.ShowSnackbar
import org.greenstand.android.TreeTracker.viewmodel.TextRef
import org.junit.Assert.assertEquals
import org.junit.Test

class TreeDetailUiTest : ComposeUiTest() {
    private val actions = mutableListOf<TreeDetailAction>()
    private val snackbars = mutableListOf<ShowSnackbar>()
    private var backClicks = 0

    @Test
    fun `detail always shows the title`() {
        setContent { TreeDetail(state = TreeDetailState()) }

        composeTestRule.onNodeWithText(string(R.string.tree_detail_title)).assertIsDisplayed()
    }

    @Test
    fun `detail without a tree shows no tree fields`() {
        setContent { TreeDetail(state = TreeDetailState()) }

        composeTestRule.onNodeWithText(string(R.string.tree_uuid_label)).assertDoesNotExist()
        composeTestRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun `detail shows the tree fields`() {
        setContent { TreeDetail(state = stateFor(pendingTree())) }

        composeTestRule.onNodeWithText(string(R.string.tree_uuid_label)).assertExists()
        composeTestRule.onNodeWithText(UUID).assertExists()
        composeTestRule.onNodeWithText(LATITUDE.toString()).assertExists()
        composeTestRule.onNodeWithText(LONGITUDE.toString()).assertExists()
        composeTestRule.onNodeWithText(CREATED_AT.toString()).assertExists()
        composeTestRule.onNodeWithText("No").assertExists()
    }

    @Test
    fun `uploaded tree is marked as uploaded`() {
        setContent { TreeDetail(state = stateFor(uploadedTree())) }

        composeTestRule.onNodeWithText("Yes").assertExists()
        composeTestRule.onNodeWithText("No").assertDoesNotExist()
    }

    @Test
    fun `note field shows the edited note`() {
        setContent { TreeDetail(state = stateFor(pendingTree()).copy(editedNote = EDITED_NOTE)) }

        composeTestRule.onNode(hasSetTextAction() and hasText(EDITED_NOTE)).assertExists()
    }

    @Test
    fun `pending tree has an editable note and save and delete buttons`() {
        setContent { TreeDetail(state = stateFor(pendingTree())) }

        composeTestRule.onNode(hasSetTextAction()).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.tree_save_note)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.tree_delete)).assertExists()
    }

    @Test
    fun `uploaded tree has a read only note and no save or delete buttons`() {
        setContent { TreeDetail(state = stateFor(uploadedTree())) }

        composeTestRule.onNode(hasText(NOTE)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(string(R.string.tree_save_note)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.tree_delete)).assertDoesNotExist()
    }

    @Test
    fun `editing the note sends UpdateNote`() {
        setContent { TreeDetail(state = stateFor(pendingTree()), onHandleAction = { actions += it }) }

        composeTestRule.onNode(hasSetTextAction()).performScrollTo().performTextClearance()

        composeTestRule.runOnIdle { assertEquals(TreeDetailAction.UpdateNote(""), actions.last()) }
    }

    @Test
    fun `clicking save sends SaveNote`() {
        setContent { TreeDetail(state = stateFor(pendingTree()), onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithText(string(R.string.tree_save_note)).performScrollTo().performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(TreeDetailAction.SaveNote), actions) }
    }

    @Test
    fun `clicking delete asks for confirmation`() {
        setContent { TreeDetail(state = stateFor(pendingTree()), onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithText(string(R.string.tree_delete)).performScrollTo().performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(TreeDetailAction.SetDeleteDialogVisibility(true)), actions)
        }
    }

    @Test
    fun `delete confirmation is hidden by default`() {
        setContent { TreeDetail(state = stateFor(pendingTree())) }

        composeTestRule.onNodeWithText(string(R.string.tree_delete_confirm_title)).assertDoesNotExist()
    }

    @Test
    fun `delete confirmation explains the deletion`() {
        setContent { TreeDetail(state = stateFor(pendingTree()).copy(showDeleteConfirmation = true)) }

        composeTestRule.onNodeWithText(string(R.string.tree_delete_confirm_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tree_delete_confirm_message)).assertIsDisplayed()
    }

    @Test
    fun `confirming the deletion sends DeleteTree`() {
        setContent {
            TreeDetail(
                state = stateFor(pendingTree()).copy(showDeleteConfirmation = true),
                onHandleAction = { actions += it },
            )
        }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(TreeDetailAction.DeleteTree), actions) }
    }

    @Test
    fun `cancelling the deletion hides the confirmation`() {
        setContent {
            TreeDetail(
                state = stateFor(pendingTree()).copy(showDeleteConfirmation = true),
                onHandleAction = { actions += it },
            )
        }

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(TreeDetailAction.SetDeleteDialogVisibility(false)), actions)
        }
    }

    @Test
    fun `back invokes onNavigateBack`() {
        setContent { TreeDetail(state = stateFor(pendingTree()), onNavigateBack = { backClicks++ }) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, backClicks) }
    }

    @Test
    fun `screen shows the tree loaded by the view model`() {
        setScreenContent(pendingTree())

        composeTestRule.onNodeWithText(UUID).assertExists()
        composeTestRule.onNode(hasSetTextAction() and hasText(NOTE)).assertExists()
    }

    @Test
    fun `saving an edited note on the screen stores it`() {
        val screen = setScreenContent(pendingTree())

        composeTestRule.onNode(hasSetTextAction()).performScrollTo().performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(EDITED_NOTE)
        composeTestRule.onNodeWithText(string(R.string.tree_save_note)).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        coVerify { screen.dao.updateTree(match { it.note == EDITED_NOTE }) }
    }

    @Test
    fun `saving a note on the screen shows the note saved snackbar`() {
        setScreenContent(pendingTree())

        composeTestRule.onNodeWithText(string(R.string.tree_save_note)).performScrollTo().performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(ShowSnackbar(TextRef.Res(R.string.tree_note_saved))), snackbars)
        }
    }

    @Test
    fun `deleting on the screen removes the tree and leaves the detail`() {
        val screen = setScreenContent(pendingTree())

        composeTestRule.onNodeWithText(string(R.string.tree_delete)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, TreeListRoute(WALLET, USER_NAME)), backStack.toList())
        }
        coVerify { screen.dao.deleteTreeById(TREE_ID) }
        coVerify { screen.treesToSyncHelper.refreshTreeCountToSync() }
    }

    @Test
    fun `cancelling the deletion on the screen keeps the tree`() {
        val screen = setScreenContent(pendingTree())

        composeTestRule.onNodeWithText(string(R.string.tree_delete)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.tree_delete_confirm_title)).assertDoesNotExist()
        composeTestRule.runOnIdle { assertEquals(TreeDetailRoute(TREE_ID), backStack.last()) }
        coVerify(exactly = 0) { screen.dao.deleteTreeById(any()) }
    }

    @Test
    fun `back on the screen returns to the tree list`() {
        setScreenContent(pendingTree())

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, TreeListRoute(WALLET, USER_NAME)), backStack.toList())
        }
    }

    private fun stateFor(tree: TreeEntity) = TreeDetailState(tree = tree, editedNote = tree.note)

    private fun setScreenContent(tree: TreeEntity): Screen {
        backStack.add(TreeListRoute(WALLET, USER_NAME))
        backStack.add(TreeDetailRoute(TREE_ID))
        val dao = mockk<TreeTrackerDAO>(relaxed = true)
        coEvery { dao.getTreesByIds(listOf(TREE_ID)) } returns listOf(tree)
        val treesToSyncHelper = mockk<TreesToSyncHelper>(relaxed = true)
        val viewModel = TreeDetailViewModel(TREE_ID, dao, treesToSyncHelper)
        setContent(inspectionMode = false) {
            LaunchedEffect(Unit) { snackbarController.events.collect { snackbars += it } }
            TreeDetailScreen(TREE_ID, viewModel)
        }
        return Screen(dao, treesToSyncHelper)
    }

    private class Screen(
        val dao: TreeTrackerDAO,
        val treesToSyncHelper: TreesToSyncHelper,
    )

    private companion object {
        const val TREE_ID = 21L
        const val WALLET = "ada@example.com"
        const val USER_NAME = "Ada Lovelace"
        const val UUID = "abc-123-def-456"
        const val NOTE = "Healthy oak tree"
        const val EDITED_NOTE = "Healthy oak tree, 2m tall"
        const val LATITUDE = 12.345678
        const val LONGITUDE = -67.890123
        val CREATED_AT: Instant = Instant.parse("2026-04-01T10:00:00Z")

        fun pendingTree() = tree(uploaded = false)

        fun uploadedTree() = tree(uploaded = true)

        private fun tree(uploaded: Boolean) =
            TreeEntity(
                uuid = UUID,
                sessionId = 1,
                photoPath = null,
                photoUrl = null,
                note = NOTE,
                latitude = LATITUDE,
                longitude = LONGITUDE,
                uploaded = uploaded,
                createdAt = CREATED_AT,
            ).apply { id = TREE_ID }
    }
}