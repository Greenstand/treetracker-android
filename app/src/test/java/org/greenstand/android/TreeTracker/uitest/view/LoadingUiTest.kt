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

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.DeterminateProgressBar
import org.greenstand.android.TreeTracker.view.showLoadingSpinner
import org.junit.Test

class LoadingUiTest : ComposeUiTest() {
    @Test
    fun `loading spinner is shown while loading`() {
        setContent { showLoadingSpinner(isLoading = true) }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun `loading spinner is hidden when not loading`() {
        setContent { showLoadingSpinner(isLoading = false) }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    @Test
    fun `determinate progress bar reports its progress`() {
        setContent { DeterminateProgressBar(progress = 0.25f, modifier = Modifier.height(100.dp)) }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f)))
            .assertExists()
    }

    @Test
    fun `determinate progress bar clamps progress above one`() {
        setContent { DeterminateProgressBar(progress = 1.5f, modifier = Modifier.height(100.dp)) }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(1f, 0f..1f)))
            .assertExists()
    }

    @Test
    fun `determinate progress bar clamps progress below zero`() {
        setContent { DeterminateProgressBar(progress = -1f, modifier = Modifier.height(100.dp)) }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..1f)))
            .assertExists()
    }
}