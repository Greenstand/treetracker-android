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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.overlay.GpsState
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsState
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SensorOverlay
import org.greenstand.android.TreeTracker.overlay.StepState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Before
import org.junit.Test

class SensorOverlayUiTest : ComposeUiTest() {
    private val diagnostics = MutableStateFlow(SensorDiagnosticsState())
    private val tracker = mockk<SensorDiagnosticsTracker>(relaxed = true)

    @Before
    fun setUp() {
        every { tracker.state } returns diagnostics
    }

    @Test
    fun `overlay shows the gps and steps sections`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("GPS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Steps").assertIsDisplayed()
    }

    @Test
    fun `without a fix the gps section says so`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("No GPS fix").assertIsDisplayed()
    }

    @Test
    fun `with a fix the gps section shows the coordinates and accuracy`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(latitude = 1.5, longitude = -2.25, accuracy = 4f))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("1.500000, -2.250000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accuracy: 4.0m").assertIsDisplayed()
        composeTestRule.onNodeWithText("No GPS fix").assertDoesNotExist()
    }

    @Test
    fun `a fix without accuracy shows only the coordinates`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(latitude = 1.5, longitude = -2.25))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("1.500000, -2.250000").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accuracy", substring = true).assertDoesNotExist()
    }

    @Test
    fun `convergence percentage is shown in the ring`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(convergencePercent = 0.5f))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun `converged status is shown`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(convergenceStatus = ConvergenceStatus.CONVERGED))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("CONVERGED").assertIsDisplayed()
    }

    @Test
    fun `not converged status is shown as converging`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(convergenceStatus = ConvergenceStatus.NOT_CONVERGED))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("CONVERGING").assertIsDisplayed()
    }

    @Test
    fun `timed out status is shown`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(convergenceStatus = ConvergenceStatus.TIMED_OUT))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("TIMED OUT").assertIsDisplayed()
    }

    @Test
    fun `no convergence status shows no status label`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("CONVERGED").assertDoesNotExist()
        composeTestRule.onNodeWithText("CONVERGING").assertDoesNotExist()
        composeTestRule.onNodeWithText("TIMED OUT").assertDoesNotExist()
    }

    @Test
    fun `standard deviations are shown with a placeholder for missing values`() {
        diagnostics.value = SensorDiagnosticsState(gps = GpsState(latStdDev = 0.000005))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("0.0000050").assertIsDisplayed()
        composeTestRule.onNodeWithText("--").assertIsDisplayed()
    }

    @Test
    fun `standard deviations are hidden when neither is known`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("--").assertDoesNotExist()
    }

    @Test
    fun `active step listener shows the step count`() {
        diagnostics.value =
            SensorDiagnosticsState(steps = StepState(deltaSteps = 42, sensorAvailable = true, listenerActive = true))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("42").assertIsDisplayed()
        composeTestRule.onNodeWithText("steps").assertIsDisplayed()
    }

    @Test
    fun `available but inactive step sensor shows idle`() {
        diagnostics.value = SensorDiagnosticsState(steps = StepState(sensorAvailable = true, listenerActive = false))
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("idle").assertIsDisplayed()
    }

    @Test
    fun `missing step sensor is reported`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText("no sensor").assertIsDisplayed()
    }

    @Test
    fun `overlay follows tracker state updates`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.runOnIdle {
            diagnostics.value = SensorDiagnosticsState(steps = StepState(sensorAvailable = true))
        }

        composeTestRule.onNodeWithText("idle").assertIsDisplayed()
        composeTestRule.onNodeWithText("no sensor").assertDoesNotExist()
    }

    @Test
    fun `clicking the header collapses the details`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).performClick()

        composeTestRule.onNodeWithText("GPS").assertDoesNotExist()
        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun `clicking the header twice expands the details again`() {
        setContent { SensorOverlay(tracker) }

        composeTestRule.onNodeWithText(TITLE).performClick()
        composeTestRule.onNodeWithText(TITLE).performClick()

        composeTestRule.onNodeWithText("GPS").assertIsDisplayed()
    }

    private companion object {
        const val TITLE = "Sensors"
    }
}