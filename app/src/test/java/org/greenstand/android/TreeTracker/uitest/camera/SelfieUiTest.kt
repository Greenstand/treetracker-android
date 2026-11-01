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
package org.greenstand.android.TreeTracker.uitest.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.camera.Selfie
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.CAPTURE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.INFO_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Tests for the stateless [Selfie] screen, with a placeholder instead of the camera. */
class SelfieUiTest : ComposeUiTest() {
    private var infoClicks = 0
    private var captureClicks = 0
    private var tutorialCompletions = 0

    private fun render(showSelfieTutorial: Boolean = false) {
        setContent {
            Selfie(
                showSelfieTutorial = showSelfieTutorial,
                onInfoClicked = { infoClicks++ },
                onCaptureClicked = { captureClicks++ },
                onTutorialCompleted = { tutorialCompletions++ },
                cameraPreview = { modifier -> Box(modifier.testTag(CAMERA_TAG)) },
            )
        }
    }

    @Test
    fun `selfie screen shows the logo camera and controls without a tutorial`() {
        render()

        composeTestRule.onNodeWithContentDescription(LOGO_DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAMERA_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.selfie_instruction)).assertDoesNotExist()
    }

    @Test
    fun `capture button reports a capture`() {
        render()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).performClick()

        assertEquals(1, captureClicks)
    }

    @Test
    fun `info button reports an info click`() {
        render()

        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).performClick()

        assertEquals(1, infoClicks)
    }

    @Test
    fun `selfie tutorial is shown when requested`() {
        render(showSelfieTutorial = true)

        composeTestRule.onNodeWithText(string(R.string.selfie_instruction)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.click_on)).assertExists()
    }

    @Test
    fun `completing the selfie tutorial reports it`() {
        render(showSelfieTutorial = true)

        composeTestRule.onNode(hasTestTag(APPROVE_BUTTON_TAG) and hasAnyAncestor(isDialog())).performClick()

        assertEquals(1, tutorialCompletions)
    }

    @Test
    fun `long pressing the logo opens dev options in debug and beta builds`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA)
        render()

        composeTestRule.onNodeWithContentDescription(LOGO_DESCRIPTION).performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, DevOptionsRoute), backStack.toList())
    }

    private companion object {
        const val CAMERA_TAG = "CameraPreview"
        const val LOGO_DESCRIPTION = "Treetracker icon"
    }
}