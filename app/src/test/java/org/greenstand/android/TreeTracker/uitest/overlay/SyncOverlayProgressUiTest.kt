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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenstand.android.TreeTracker.overlay.DebugOverlayHost
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.NoOpSyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsState
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncOverlay
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SyncStep
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the progress bars, item counts, error affordances and dragging of [SyncOverlay],
 * plus how [DebugOverlayHost] shows it next to the sensor overlay.
 */
class SyncOverlayProgressUiTest : ComposeUiTest() {
    private val tracker = SyncProgressTracker()
    private val totalSteps = SyncStep.entries.size

    private val anyProgressBar = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    private fun progressBar(progress: Float) = hasProgressBarRangeInfo(ProgressBarRangeInfo(progress, 0f..1f))

    @Test
    fun `overall progress bar starts empty`() {
        tracker.startSync()
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNode(progressBar(0f)).assertExists()
    }

    @Test
    fun `overall progress bar fills with the share of completed steps`() {
        tracker.startSync()
        listOf(SyncStep.MESSAGES, SyncStep.DEVICE_CONFIG).forEach {
            tracker.startStep(it)
            tracker.completeStep(it)
        }
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("2/$totalSteps").assertIsDisplayed()
        composeTestRule.onNode(progressBar(2f / totalSteps)).assertExists()
    }

    @Test
    fun `failed steps do not count towards the overall progress`() {
        tracker.startSync()
        tracker.failStep(SyncStep.MESSAGES, STEP_ERROR)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("0/$totalSteps").assertIsDisplayed()
        composeTestRule.onNode(progressBar(0f)).assertExists()
    }

    @Test
    fun `running step with known items shows its own progress bar`() {
        tracker.startSync()
        tracker.startStep(SyncStep.TREES)
        tracker.updateStepProgress(SyncStep.TREES, completed = 2, total = 5)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onAllNodes(anyProgressBar).assertCountEquals(2)
        composeTestRule.onNode(progressBar(0.4f)).assertExists()
    }

    @Test
    fun `running step without known items shows no count or progress bar`() {
        tracker.startSync()
        tracker.startStep(SyncStep.TREES)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onAllNodes(anyProgressBar).assertCountEquals(1)
        composeTestRule.onNodeWithText("0/0").assertDoesNotExist()
    }

    @Test
    fun `completed step keeps its item count but drops its progress bar`() {
        tracker.startSync()
        tracker.startStep(SyncStep.TREES)
        tracker.updateStepProgress(SyncStep.TREES, completed = 5, total = 5)
        tracker.completeStep(SyncStep.TREES)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText("5/5").assertIsDisplayed()
        composeTestRule.onAllNodes(anyProgressBar).assertCountEquals(1)
    }

    @Test
    fun `item count follows progress updates while the overlay is shown`() {
        tracker.startSync()
        tracker.startStep(SyncStep.LOCATIONS)
        tracker.updateStepProgress(SyncStep.LOCATIONS, completed = 1, total = 4)
        setContent { SyncOverlay(tracker) }
        composeTestRule.onNodeWithText("1/4").assertIsDisplayed()

        composeTestRule.runOnIdle { tracker.updateStepProgress(SyncStep.LOCATIONS, completed = 3, total = 4) }

        composeTestRule.onNodeWithText("3/4").assertIsDisplayed()
        composeTestRule.onNode(progressBar(0.75f)).assertExists()
    }

    @Test
    fun `failed step with an error message can be expanded`() {
        tracker.startSync()
        tracker.failStep(SyncStep.USERS, STEP_ERROR)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(SyncStep.USERS.displayName).assertHasClickAction()
    }

    @Test
    fun `failed step without an error message cannot be expanded`() {
        tracker.startSync()
        tracker.failStep(SyncStep.USERS, error = null)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(SyncStep.USERS.displayName).assertHasNoClickAction()
    }

    @Test
    fun `pending steps cannot be expanded`() {
        tracker.startSync()
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(SyncStep.SESSIONS.displayName).assertHasNoClickAction()
    }

    @Test
    fun `collapsing hides the overall error but keeps the error status`() {
        tracker.startSync()
        tracker.endSync(error = OVERALL_ERROR)
        setContent { SyncOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).performClick()

        composeTestRule.onNodeWithText(OVERALL_ERROR).assertDoesNotExist()
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
    }

    @Test
    fun `a new sync clears the previous error`() {
        tracker.startSync()
        tracker.endSync(error = OVERALL_ERROR)
        setContent { SyncOverlay(tracker) }
        composeTestRule.onNodeWithText(OVERALL_ERROR).assertIsDisplayed()

        composeTestRule.runOnIdle { tracker.startSync() }

        composeTestRule.onNodeWithText(OVERALL_ERROR).assertDoesNotExist()
        composeTestRule.onNodeWithText("0/$totalSteps").assertIsDisplayed()
    }

    @Test
    fun `release builds no-op tracker never reports an active sync`() {
        val noOpTracker = NoOpSyncProgressTracker()
        noOpTracker.startSync()
        noOpTracker.startStep(SyncStep.TREES)
        noOpTracker.failStep(SyncStep.TREES, STEP_ERROR)
        setContent { SyncOverlay(noOpTracker) }

        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
        composeTestRule.onNodeWithText(SyncStep.TREES.displayName).assertHasNoClickAction()
    }

    @Test
    fun `dragging the overlay moves it`() {
        setContent { SyncOverlay(tracker) }
        val before = composeTestRule.onNodeWithText(TITLE).getBoundsInRoot()

        composeTestRule.onNodeWithText(TITLE).performTouchInput {
            swipe(start = center, end = center + Offset(DRAG_PX, DRAG_PX), durationMillis = 300)
        }

        val after = composeTestRule.onNodeWithText(TITLE).getBoundsInRoot()
        assertTrue("overlay should move right, was $before now $after", after.left > before.left)
        assertTrue("overlay should move down, was $before now $after", after.top > before.top)
    }

    @Test
    fun `debug host shows the sync and sensor overlays together`() {
        val overlayManager = DebugOverlayManager()
        val sensorTracker = mockk<SensorDiagnosticsTracker>(relaxed = true)
        every { sensorTracker.state } returns MutableStateFlow(SensorDiagnosticsState())
        overlayManager.setSyncOverlay(true)
        overlayManager.setSensorOverlay(true)

        setContent {
            DebugOverlayHost(
                overlayManager = overlayManager,
                syncProgressTracker = tracker,
                sensorDiagnosticsTracker = sensorTracker,
            )
        }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("GPS").assertIsDisplayed()
    }

    @Test
    fun `debug host hides the sync overlay when it is turned off elsewhere`() {
        val overlayManager = DebugOverlayManager()
        val sensorTracker = mockk<SensorDiagnosticsTracker>(relaxed = true)
        every { sensorTracker.state } returns MutableStateFlow(SensorDiagnosticsState())
        overlayManager.setSyncOverlay(true)
        setContent {
            DebugOverlayHost(
                overlayManager = overlayManager,
                syncProgressTracker = tracker,
                sensorDiagnosticsTracker = sensorTracker,
            )
        }
        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()

        composeTestRule.runOnIdle { overlayManager.setSyncOverlay(false) }

        composeTestRule.onAllNodesWithText(TITLE).assertCountEquals(0)
    }

    private companion object {
        const val TITLE = "Sync Progress"
        const val OVERALL_ERROR = "Network unavailable"
        const val STEP_ERROR = "Upload failed: 500"
        const val DRAG_PX = 200f
    }
}