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

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.capture.CREATE_FAKE_TREES_BUTTON_TAG
import org.greenstand.android.TreeTracker.capture.TreeCapture
import org.greenstand.android.TreeTracker.capture.TreeCaptureState
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.CAPTURE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.INFO_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.USER_IMAGE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Tests for the stateless [TreeCapture] screen, with a placeholder instead of the camera. */
class TreeCaptureUiTest : ComposeUiTest() {
    private val clicks = mutableListOf<String>()

    private fun render(state: TreeCaptureState = TreeCaptureState(profilePicUrl = "/no/such/me.jpg")) {
        setContent {
            TreeCapture(
                state = state,
                onBackClicked = { clicks += BACK },
                onCaptureClicked = { clicks += CAPTURE },
                onInfoClicked = { clicks += INFO },
                onBadGpsRetryClicked = { clicks += BAD_GPS_RETRY },
                onBadGpsExitClicked = { clicks += BAD_GPS_EXIT },
                onTutorialCompleted = { clicks += TUTORIAL_DONE },
                onProfileClicked = { clicks += PROFILE },
                onCreateFakeTreesClicked = { clicks += FAKE_TREES },
                cameraPreview = { modifier -> Box(modifier.testTag(CAMERA_TAG)) },
            )
        }
    }

    @Test
    fun `idle screen shows the camera and controls without dialogs or progress`() {
        render()

        composeTestRule.onNodeWithTag(CAMERA_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertDoesNotExist()
    }

    @Test
    fun `capture button reports a capture`() {
        render()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).performClick()

        assertEquals(listOf(CAPTURE), clicks)
    }

    @Test
    fun `back arrow reports a back click`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf(BACK), clicks)
    }

    @Test
    fun `info button reports an info click`() {
        render()

        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).performClick()

        assertEquals(listOf(INFO), clicks)
    }

    @Test
    fun `profile picture reports a profile click`() {
        render()

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).performClick()

        assertEquals(listOf(PROFILE), clicks)
    }

    @Test
    fun `getting a location shows the convergence progress and disables capture`() {
        render(state = TreeCaptureState(isGettingLocation = true, convergencePercentage = 0.25f))

        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tracking_progress_message)).assertIsDisplayed()
        composeTestRule.onNodeWithText("25 %").assertIsDisplayed()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).performClick()
        assertEquals(emptyList<String>(), clicks)
    }

    @Test
    fun `creating fake trees shows the progress but keeps capture enabled`() {
        render(state = TreeCaptureState(isCreatingFakeTrees = true))

        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText("0 %").assertIsDisplayed()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).performClick()
        assertEquals(listOf(CAPTURE), clicks)
    }

    @Test
    fun `bad gps dialog is shown when the location is unavailable`() {
        render(state = TreeCaptureState(isLocationAvailable = false))

        composeTestRule.onNodeWithText(string(R.string.poor_gps_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.poor_gps_message)).assertIsDisplayed()
    }

    @Test
    fun `bad gps dialog is hidden while the location is available or unknown`() {
        render(state = TreeCaptureState(isLocationAvailable = true))

        composeTestRule.onNodeWithText(string(R.string.poor_gps_header)).assertDoesNotExist()
    }

    @Test
    fun `approving the bad gps dialog retries`() {
        render(state = TreeCaptureState(isLocationAvailable = false))

        composeTestRule.onNode(hasTestTag(APPROVE_BUTTON_TAG) and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf(BAD_GPS_RETRY), clicks)
    }

    @Test
    fun `declining the bad gps dialog exits`() {
        render(state = TreeCaptureState(isLocationAvailable = false))

        composeTestRule.onNode(hasTestTag(DECLINE_BUTTON_TAG) and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf(BAD_GPS_EXIT), clicks)
    }

    @Test
    fun `capture tutorial is shown on the first capture`() {
        render(state = TreeCaptureState(showCaptureTutorial = true))

        composeTestRule.onNodeWithText(string(R.string.tracking_instruction)).assertExists()
    }

    @Test
    fun `capture tutorial is hidden once dismissed`() {
        render(state = TreeCaptureState(showCaptureTutorial = false))

        composeTestRule.onNodeWithText(string(R.string.tracking_instruction)).assertDoesNotExist()
    }

    @Test
    fun `completing the capture tutorial reports it`() {
        render(state = TreeCaptureState(showCaptureTutorial = true))

        composeTestRule.onNode(hasTestTag(APPROVE_BUTTON_TAG) and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf(TUTORIAL_DONE), clicks)
    }

    @Test
    fun `fake trees button reports a click in debug and beta builds`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA)
        render()

        composeTestRule.onNodeWithTag(CREATE_FAKE_TREES_BUTTON_TAG).performClick()

        assertEquals(listOf(FAKE_TREES), clicks)
    }

    @Test
    fun `fake trees button is inert while fake trees are being created`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA)
        render(state = TreeCaptureState(isCreatingFakeTrees = true))

        composeTestRule.onNodeWithTag(CREATE_FAKE_TREES_BUTTON_TAG).performClick()

        assertEquals(emptyList<String>(), clicks)
    }

    private companion object {
        const val CAMERA_TAG = "CameraPreview"
        const val BACK = "back"
        const val CAPTURE = "capture"
        const val INFO = "info"
        const val BAD_GPS_RETRY = "badGpsRetry"
        const val BAD_GPS_EXIT = "badGpsExit"
        const val TUTORIAL_DONE = "tutorialDone"
        const val PROFILE = "profile"
        const val FAKE_TREES = "fakeTrees"
    }
}