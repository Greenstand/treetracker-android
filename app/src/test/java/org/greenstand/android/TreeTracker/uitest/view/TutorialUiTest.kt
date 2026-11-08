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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.CAPTURE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ImageCapturing
import org.greenstand.android.TreeTracker.view.SelfieTutorial
import org.greenstand.android.TreeTracker.view.TestTags
import org.greenstand.android.TreeTracker.view.TreeCaptureReviewTutorial
import org.greenstand.android.TreeTracker.view.TreeCaptureTutorial
import org.greenstand.android.TreeTracker.view.TutorialDialog
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialUiTest : ComposeUiTest() {
    private var completions = 0

    @Test
    fun `selfie tutorial explains how to take a selfie`() {
        setContent { SelfieTutorial(onCompleteClick = {}) }

        composeTestRule.onNodeWithText(string(R.string.click_on)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.selfie_instruction)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).assertExists()
    }

    @Test
    fun `selfie tutorial completes when the approve button is clicked`() {
        setContent { SelfieTutorial(onCompleteClick = { completions++ }) }

        composeTestRule.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun `the example capture button in the tutorial is hidden from accessibility`() {
        setContent { SelfieTutorial(onCompleteClick = { completions++ }) }

        // Only a picture of the button, so tools reading the screen do not mistake it for the real one.
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `tree capture tutorial explains how to capture a tree`() {
        setContent { TreeCaptureTutorial(onCompleteClick = {}) }

        composeTestRule.onNodeWithText(string(R.string.click_on)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.tracking_instruction)).assertExists()
    }

    @Test
    fun `tree capture tutorial completes when the approve button is clicked`() {
        setContent { TreeCaptureTutorial(onCompleteClick = { completions++ }) }

        composeTestRule.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun `tree capture review tutorial explains note, save and recapture`() {
        setContent { TreeCaptureReviewTutorial(onCompleteClick = {}) }

        composeTestRule.onNodeWithText(string(R.string.tree_capture_review_text)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.capture_tutorial)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.recapture_tutorial)).assertExists()
        composeTestRule.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).assertExists()
    }

    @Test
    fun `tutorial dialog renders its content and completes on approve`() {
        setContent {
            TutorialDialog(
                content = { Text(CONTENT) },
                onCompleteClick = { completions++ },
            )
        }

        composeTestRule.onNodeWithText(CONTENT).assertExists()
        composeTestRule.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun `the example buttons in the review tutorial are hidden from accessibility`() {
        setContent { TreeCaptureReviewTutorial(onCompleteClick = { completions++ }) }

        // Only pictures of the note, save and recapture buttons, so tools reading the screen do not
        // mistake them for the real ones under the dialog.
        composeTestRule.onNodeWithText(string(R.string.note)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onAllNodesWithTag(TestTags.TUTORIAL_DONE_BUTTON).assertCountEquals(1)
    }

    @Test
    fun `the review tutorial completes with its own approve button`() {
        setContent { TreeCaptureReviewTutorial(onCompleteClick = { completions++ }) }

        composeTestRule.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun `tutorial dialog without content only offers the approve button`() {
        setContent { TutorialDialog(onCompleteClick = { completions++ }) }

        composeTestRule.onAllNodesWithTag(TestTags.TUTORIAL_DONE_BUTTON).assertCountEquals(1)
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `image capturing shows the given instruction and hides the example capture button`() {
        setContent {
            ImageCapturing(
                image = painterResource(id = R.drawable.selfie_illustration),
                text = CONTENT,
            )
        }

        composeTestRule.onNodeWithText(string(R.string.click_on)).assertExists()
        composeTestRule.onNodeWithText(CONTENT).assertExists()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertDoesNotExist()
    }

    private companion object {
        const val CONTENT = "Tutorial content"
    }
}