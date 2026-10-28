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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenstand.android.TreeTracker.overlay.DebugOverlayHost
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsState
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebugOverlayHostUiTest : ComposeUiTest() {
    private val overlayManager = DebugOverlayManager()
    private val syncProgressTracker = SyncProgressTracker()
    private val sensorTracker = mockk<SensorDiagnosticsTracker>(relaxed = true)

    private var showHost by mutableStateOf(true)

    @Before
    fun setUp() {
        every { sensorTracker.state } returns MutableStateFlow(SensorDiagnosticsState())
    }

    @Test
    fun `only the debug toggle is shown initially`() {
        setHostContent()

        composeTestRule.onNodeWithText(TOGGLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(SYNC_ITEM).assertDoesNotExist()
        composeTestRule.onNodeWithText(SENSORS_ITEM).assertDoesNotExist()
        composeTestRule.onNodeWithText(SYNC_OVERLAY_TITLE).assertDoesNotExist()
    }

    @Test
    fun `clicking the toggle opens the overlay menu`() {
        setHostContent()

        composeTestRule.onNodeWithText(TOGGLE).performClick()

        composeTestRule.onNodeWithText(SYNC_ITEM).assertIsDisplayed()
        composeTestRule.onNodeWithText(SENSORS_ITEM).assertIsDisplayed()
    }

    @Test
    fun `clicking the toggle twice closes the overlay menu`() {
        setHostContent()

        composeTestRule.onNodeWithText(TOGGLE).performClick()
        composeTestRule.onNodeWithText(TOGGLE).performClick()

        composeTestRule.onNodeWithText(SYNC_ITEM).assertDoesNotExist()
    }

    @Test
    fun `the sync menu item toggles the sync overlay`() {
        setHostContent()

        composeTestRule.onNodeWithText(TOGGLE).performClick()
        composeTestRule.onNodeWithText(SYNC_ITEM).performClick()

        composeTestRule.onNodeWithText(SYNC_OVERLAY_TITLE).assertIsDisplayed()
        composeTestRule.runOnIdle { assertTrue(overlayManager.showSyncOverlay.value) }
    }

    @Test
    fun `the sync menu item hides a visible sync overlay`() {
        overlayManager.setSyncOverlay(true)
        setHostContent()

        composeTestRule.onNodeWithText(TOGGLE).performClick()
        composeTestRule.onNodeWithText(SYNC_ITEM).performClick()

        composeTestRule.onNodeWithText(SYNC_OVERLAY_TITLE).assertDoesNotExist()
        composeTestRule.runOnIdle { assertFalse(overlayManager.showSyncOverlay.value) }
    }

    @Test
    fun `the sensors menu item shows the sensor overlay and starts the tracker`() {
        setHostContent()

        composeTestRule.onNodeWithText(TOGGLE).performClick()
        composeTestRule.onNodeWithText(SENSORS_ITEM).performClick()

        composeTestRule.onNodeWithText(SENSOR_OVERLAY_SECTION).assertIsDisplayed()
        composeTestRule.runOnIdle { assertTrue(overlayManager.showSensorOverlay.value) }
        verify(exactly = 1) { sensorTracker.start() }
    }

    @Test
    fun `sensor tracker is stopped and never started while the sensor overlay is hidden`() {
        setHostContent()

        composeTestRule.waitForIdle()

        verify { sensorTracker.stop() }
        verify(exactly = 0) { sensorTracker.start() }
    }

    @Test
    fun `visible sensor overlay starts the tracker`() {
        overlayManager.setSensorOverlay(true)
        setHostContent()

        composeTestRule.onNodeWithText(SENSOR_OVERLAY_SECTION).assertIsDisplayed()
        verify(exactly = 1) { sensorTracker.start() }
        verify(exactly = 0) { sensorTracker.stop() }
    }

    @Test
    fun `hiding the sensor overlay stops the tracker`() {
        overlayManager.setSensorOverlay(true)
        setHostContent()
        composeTestRule.waitForIdle()
        clearMocks(sensorTracker, answers = false)

        composeTestRule.runOnIdle { overlayManager.setSensorOverlay(false) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(SENSOR_OVERLAY_SECTION).assertDoesNotExist()
        verify { sensorTracker.stop() }
        verify(exactly = 0) { sensorTracker.start() }
    }

    @Test
    fun `leaving composition with the sensor overlay visible stops the tracker`() {
        overlayManager.setSensorOverlay(true)
        setHostContent()
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { showHost = false }
        composeTestRule.waitForIdle()

        verify(exactly = 1) { sensorTracker.stop() }
    }

    private fun setHostContent() {
        setContent {
            if (showHost) {
                DebugOverlayHost(
                    overlayManager = overlayManager,
                    syncProgressTracker = syncProgressTracker,
                    sensorDiagnosticsTracker = sensorTracker,
                )
            }
        }
    }

    private companion object {
        const val TOGGLE = "D"
        const val SYNC_ITEM = "Sync"
        const val SENSORS_ITEM = "Sensors"
        const val SYNC_OVERLAY_TITLE = "Sync Progress"
        const val SENSOR_OVERLAY_SECTION = "GPS"
    }
}