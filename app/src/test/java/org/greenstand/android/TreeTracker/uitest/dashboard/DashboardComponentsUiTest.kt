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
package org.greenstand.android.TreeTracker.uitest.dashboard

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.dashboard.DashBoardButton
import org.greenstand.android.TreeTracker.dashboard.Dashboard
import org.greenstand.android.TreeTracker.dashboard.DashboardState
import org.greenstand.android.TreeTracker.dashboard.DashboardUploadProgressBar
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.TestTags
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the dashboard building blocks and how [Dashboard] follows state changes. */
@OptIn(ExperimentalComposeApi::class)
class DashboardComponentsUiTest : ComposeUiTest() {
    private fun progressBar() = composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

    @Test
    fun `dashboard button shows its label and reports clicks`() {
        var clicks = 0
        setContent {
            DashBoardButton(
                text = BUTTON_LABEL,
                onClick = { clicks++ },
                colors = AppButtonColors.ProgressGreen,
                image = painterResource(id = R.drawable.track_icon),
            )
        }

        composeTestRule.onNodeWithText(BUTTON_LABEL).assertIsDisplayed().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `upload progress bar exposes its progress`() {
        setContent { DashboardUploadProgressBar(progress = 0.25f) }

        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0.25f, 0f..1f))
    }

    @Test
    fun `upload progress bar animates to a new progress`() {
        var progress by mutableStateOf(0.25f)
        setContent { DashboardUploadProgressBar(progress = progress) }

        progress = 0.8f

        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0.8f, 0f..1f))
    }

    @Test
    fun `dashboard counts follow state updates`() {
        var state by mutableStateOf(DashboardState(treesSynced = 3, treesRemainingToSync = 9, totalTreesToSync = 12))
        setContent { Dashboard(state = state) }
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertIsDisplayed()

        state = DashboardState(treesSynced = 12, treesRemainingToSync = 0, totalTreesToSync = 0)

        composeTestRule.onNodeWithText("12").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertDoesNotExist()
        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun `sync reminder can only be acknowledged, not declined`() {
        setContent { Dashboard(state = DashboardState(), showSyncReminderDialog = true) }

        composeTestRule.onNode(hasTestTag(TestTags.DIALOG_POSITIVE_BUTTON) and hasAnyAncestor(isDialog())).assertExists()
        composeTestRule.onNode(hasTestTag(TestTags.DIALOG_NEGATIVE_BUTTON) and hasAnyAncestor(isDialog())).assertDoesNotExist()
    }

    private companion object {
        const val BUTTON_LABEL = "Plant"
    }
}