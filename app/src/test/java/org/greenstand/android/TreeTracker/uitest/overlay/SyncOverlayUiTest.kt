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
package org.greenstand.android.TreeTracker.uitest.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.overlay.SyncOverlay
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SyncStep
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Test

class SyncOverlayUiTest : ComposeUiTest() {
    private val tracker = SyncProgressTracker()

    @Test
    fun `idle overlay shows the title and reports done`() {
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun `overlay lists every sync step`() {
        setContent { SyncOverlay(tracker) }

        SyncStep.entries.forEach { step ->
            composeTestRule.onNodeWithText(step.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun `active sync reports completed steps out of the total`() {
        tracker.startSync()
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("0/${SyncStep.entries.size}").assertIsDisplayed()
    }

    @Test
    fun `completing a step advances the progress count`() {
        tracker.startSync()
        setContent { SyncOverlay(tracker) }

        composeTestRule.runOnIdle {
            tracker.startStep(SyncStep.MESSAGES)
            tracker.completeStep(SyncStep.MESSAGES)
        }

        composeTestRule.onNodeWithText("1/${SyncStep.entries.size}").assertIsDisplayed()
    }

    @Test
    fun `step item progress is shown once the step knows its total`() {
        tracker.startSync()
        tracker.startStep(SyncStep.TREES)
        tracker.updateStepProgress(SyncStep.TREES, completed = 2, total = 5)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("2/5").assertIsDisplayed()
    }

    @Test
    fun `sync that ended with an error reports the error`() {
        tracker.startSync()
        tracker.endSync(error = OVERALL_ERROR)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
        composeTestRule.onNodeWithText(OVERALL_ERROR).assertIsDisplayed()
    }

    @Test
    fun `sync that ended without an error reports done`() {
        tracker.startSync()
        tracker.endSync()
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error").assertDoesNotExist()
    }

    @Test
    fun `a failed step hides its error until the step is clicked`() {
        tracker.startSync()
        tracker.failStep(SyncStep.TREES, STEP_ERROR)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(STEP_ERROR).assertDoesNotExist()

        composeTestRule.onNodeWithText(SyncStep.TREES.displayName).performClick()

        composeTestRule.onNodeWithText(STEP_ERROR).assertIsDisplayed()
    }

    @Test
    fun `clicking a failed step again hides its error`() {
        tracker.startSync()
        tracker.failStep(SyncStep.TREES, STEP_ERROR)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(SyncStep.TREES.displayName).performClick()
        composeTestRule.onNodeWithText(SyncStep.TREES.displayName).performClick()

        composeTestRule.onNodeWithText(STEP_ERROR).assertDoesNotExist()
    }

    @Test
    fun `clicking the header collapses the step list`() {
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).performClick()

        composeTestRule.onNodeWithText(SyncStep.MESSAGES.displayName).assertDoesNotExist()
        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun `clicking the header twice expands the step list again`() {
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).performClick()
        composeTestRule.onNodeWithText(TITLE).performClick()

        composeTestRule.onNodeWithText(SyncStep.MESSAGES.displayName).assertIsDisplayed()
    }

    private companion object {
        const val TITLE = "Sync Progress"
        const val OVERALL_ERROR = "Network unavailable"
        const val STEP_ERROR = "Upload failed: 500"
    }
}