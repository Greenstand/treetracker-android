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
import org.greenstand.android.TreeTracker.view.SelfieTutorial
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
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertExists()
    }

    @Test
    fun `selfie tutorial completes when the approve button is clicked`() {
        setContent { SelfieTutorial(onCompleteClick = { completions++ }) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun `the example capture button in the tutorial is not clickable`() {
        setContent { SelfieTutorial(onCompleteClick = { completions++ }) }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(0, completions) }
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

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    @Test
    fun `tree capture review tutorial explains note, save and recapture`() {
        setContent { TreeCaptureReviewTutorial(onCompleteClick = {}) }

        composeTestRule.onNodeWithText(string(R.string.tree_capture_review_text)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.note)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.capture_tutorial)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.recapture_tutorial)).assertExists()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertExists()
        // The example save button plus the dialog's own complete button.
        composeTestRule.onAllNodesWithTag(APPROVE_BUTTON_TAG).assertCountEquals(2)
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
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, completions) }
    }

    private companion object {
        const val CONTENT = "Tutorial content"
    }
}