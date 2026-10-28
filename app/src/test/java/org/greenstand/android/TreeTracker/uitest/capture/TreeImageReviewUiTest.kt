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
package org.greenstand.android.TreeTracker.uitest.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.capture.TreeImageReview
import org.greenstand.android.TreeTracker.capture.TreeImageReviewAction
import org.greenstand.android.TreeTracker.capture.TreeImageReviewState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.INFO_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the stateless [TreeImageReview] screen. */
class TreeImageReviewUiTest : ComposeUiTest() {
    private val actions = mutableListOf<TreeImageReviewAction>()

    private val approveInDialog = hasTestTag(APPROVE_BUTTON_TAG) and hasAnyAncestor(isDialog())
    private val declineInDialog = hasTestTag(DECLINE_BUTTON_TAG) and hasAnyAncestor(isDialog())

    private fun render(state: TreeImageReviewState = TreeImageReviewState(treeImagePath = "/no/such/tree.jpg")) {
        setContent {
            TreeImageReview(
                state = state,
                onHandleAction = { actions += it },
            )
        }
    }

    @Test
    fun `review shows the note button and review controls without dialogs`() {
        render()

        composeTestRule.onNodeWithText(string(R.string.note)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun `note button opens the note dialog`() {
        render()

        composeTestRule.onNodeWithText(string(R.string.note)).performClick()

        assertEquals(listOf(TreeImageReviewAction.SetDialogState(true)), actions)
    }

    @Test
    fun `decline button navigates back`() {
        render()

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        assertEquals(listOf(TreeImageReviewAction.NavigateBack), actions)
    }

    @Test
    fun `approve button asks to navigate forward`() {
        render()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        assertEquals(listOf(TreeImageReviewAction.CheckIfCanNavigateForward), actions)
    }

    @Test
    fun `info button opens the review tutorial`() {
        render()

        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).performClick()

        assertEquals(listOf(TreeImageReviewAction.UpdateReviewTutorialDialog(true)), actions)
    }

    @Test
    fun `open note dialog shows its title and the current note`() {
        render(state = TreeImageReviewState(isDialogOpen = true, note = TREE_NOTE))

        composeTestRule.onNodeWithText(string(R.string.add_note_to_tree)).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertExists()
        composeTestRule.onNodeWithText(TREE_NOTE).assertExists()
    }

    @Test
    fun `typing in the note dialog updates the note`() {
        render(state = TreeImageReviewState(isDialogOpen = true))

        composeTestRule.onNode(hasSetTextAction()).performTextInput(TREE_NOTE)

        assertEquals(listOf(TreeImageReviewAction.UpdateNote(TREE_NOTE)), actions)
    }

    @Test
    fun `approving the note dialog adds the note`() {
        render(state = TreeImageReviewState(isDialogOpen = true, note = TREE_NOTE))

        composeTestRule.onNode(approveInDialog).performClick()

        assertEquals(listOf(TreeImageReviewAction.AddNote), actions)
    }

    @Test
    fun `declining the note dialog closes it`() {
        render(state = TreeImageReviewState(isDialogOpen = true, note = TREE_NOTE))

        composeTestRule.onNode(declineInDialog).performClick()

        assertEquals(listOf(TreeImageReviewAction.SetDialogState(false)), actions)
    }

    @Test
    fun `review tutorial is shown when requested`() {
        render(state = TreeImageReviewState(showReviewTutorial = true))

        composeTestRule.onNodeWithText(string(R.string.tree_capture_review_text)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.capture_tutorial)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.recapture_tutorial)).assertExists()
    }

    @Test
    fun `review tutorial is hidden once dismissed`() {
        render(state = TreeImageReviewState(showReviewTutorial = false))

        composeTestRule.onNodeWithText(string(R.string.tree_capture_review_text)).assertDoesNotExist()
    }

    @Test
    fun `completing the review tutorial dismisses it`() {
        render(state = TreeImageReviewState(showReviewTutorial = true))

        // The tutorial illustrates the approve button with an inert copy, so click every
        // approve button in the dialog: only the real one reports an action.
        val approveButtons = composeTestRule.onAllNodes(approveInDialog)
        approveButtons.fetchSemanticsNodes().indices.forEach { index ->
            approveButtons[index].performClick()
        }

        assertEquals(listOf(TreeImageReviewAction.UpdateReviewTutorialDialog(false)), actions)
    }

    private companion object {
        const val TREE_NOTE = "Young mango tree"
    }
}