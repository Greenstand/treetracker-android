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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.capture.CaptureCustomLoading
import org.greenstand.android.TreeTracker.capture.TreeCapture
import org.greenstand.android.TreeTracker.capture.TreeCaptureState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Test

/** Tests for the [CaptureCustomLoading] GPS convergence panel, alone and inside [TreeCapture]. */
class CaptureCustomLoadingUiTest : ComposeUiTest() {
    @Test
    fun `nothing is drawn while not loading`() {
        setContent { CaptureCustomLoading(isLoading = false, progress = 0.5f) }

        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertDoesNotExist()
        composeTestRule.onNodeWithText("50 %").assertDoesNotExist()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun `loading shows the header message spinner and percentage`() {
        setContent { CaptureCustomLoading(isLoading = true, progress = 0.5f) }

        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.tracking_progress_message)).assertIsDisplayed()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        composeTestRule.onNodeWithText("50 %").assertIsDisplayed()
    }

    @Test
    fun `percentage is truncated rather than rounded`() {
        setContent { CaptureCustomLoading(isLoading = true, progress = 0.999f) }

        composeTestRule.onNodeWithText("99 %").assertIsDisplayed()
    }

    @Test
    fun `full convergence shows one hundred percent`() {
        setContent { CaptureCustomLoading(isLoading = true, progress = 1f) }

        composeTestRule.onNodeWithText("100 %").assertIsDisplayed()
    }

    @Test
    fun `percentage follows the convergence as it changes`() {
        var progress by mutableStateOf(0.1f)
        setContent { CaptureCustomLoading(isLoading = true, progress = progress) }
        composeTestRule.onNodeWithText("10 %").assertIsDisplayed()

        progress = 0.75f

        composeTestRule.onNodeWithText("75 %").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 %").assertDoesNotExist()
    }

    @Test
    fun `panel disappears when loading ends`() {
        var isLoading by mutableStateOf(true)
        setContent { CaptureCustomLoading(isLoading = isLoading, progress = 0.3f) }
        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertIsDisplayed()

        isLoading = false

        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertDoesNotExist()
    }

    @Test
    fun `capture screen shows a single panel while locating and creating fake trees at once`() {
        setContent {
            TreeCapture(
                state = TreeCaptureState(isGettingLocation = true, isCreatingFakeTrees = true, convergencePercentage = 0.4f),
            )
        }

        composeTestRule.onAllNodesWithText(string(R.string.tracking_progress_header)).assertCountEquals(1)
        composeTestRule.onNodeWithText("40 %").assertIsDisplayed()
    }

    @Test
    fun `capture screen hides the panel once the location is captured`() {
        var state by mutableStateOf(TreeCaptureState(isGettingLocation = true, convergencePercentage = 0.9f))
        setContent { TreeCapture(state = state) }
        composeTestRule.onNodeWithText("90 %").assertIsDisplayed()

        state = state.copy(isGettingLocation = false)

        composeTestRule.onNodeWithText(string(R.string.tracking_progress_header)).assertDoesNotExist()
    }
}