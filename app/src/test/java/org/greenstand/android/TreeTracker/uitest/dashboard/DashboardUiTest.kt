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

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.dashboard.Dashboard
import org.greenstand.android.TreeTracker.dashboard.DashboardState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardUiTest : ComposeUiTest() {
    private val clicks = mutableListOf<String>()

    private val state =
        DashboardState(
            treesSynced = 146,
            treesRemainingToSync = 51,
            totalTreesToSync = 200,
        )

    private fun setDashboard(
        state: DashboardState = this.state,
        showSyncReminderDialog: Boolean = false,
    ) {
        setContent {
            Dashboard(
                state = state,
                showSyncReminderDialog = showSyncReminderDialog,
                onSyncClicked = { clicks += SYNC },
                onOrgClicked = { clicks += ORG },
                onCaptureClicked = { clicks += CAPTURE },
                onDialogConfirm = { clicks += DIALOG_CONFIRM },
                onMessagesClicked = { clicks += MESSAGES },
                onSettingsClicked = { clicks += SETTINGS },
            )
        }
    }

    private fun progressBar() = composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

    @Test
    fun `shows synced and remaining tree counts`() {
        setDashboard()

        composeTestRule.onNodeWithText("146").assertIsDisplayed()
        composeTestRule.onNodeWithText("51").assertIsDisplayed()
    }

    @Test
    fun `shows upload messages and track buttons`() {
        setDashboard()

        composeTestRule.onNodeWithText(string(R.string.upload)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.messages)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.track)).assertIsDisplayed()
    }

    @Test
    fun `upload progress reflects remaining over total trees`() {
        setDashboard()

        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(state.uploadProgress, 0f..1f))
    }

    @Test
    fun `upload progress is empty when there is nothing to sync`() {
        setDashboard(DashboardState())

        progressBar().assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f))
    }

    @Test
    fun `upload click invokes onSyncClicked`() {
        setDashboard()

        composeTestRule.onNodeWithText(string(R.string.upload)).performClick()

        assertEquals(listOf(SYNC), clicks)
    }

    @Test
    fun `messages click invokes onMessagesClicked`() {
        setDashboard()

        composeTestRule.onNodeWithText(string(R.string.messages)).performClick()

        assertEquals(listOf(MESSAGES), clicks)
    }

    @Test
    fun `track click invokes onCaptureClicked`() {
        setDashboard()

        composeTestRule.onNodeWithText(string(R.string.track)).performClick()

        assertEquals(listOf(CAPTURE), clicks)
    }

    @Test
    fun `settings icon click invokes onSettingsClicked`() {
        setDashboard()

        composeTestRule.onNode(hasClickAction()).performClick()

        assertEquals(listOf(SETTINGS), clicks)
    }

    @Test
    fun `sync reminder dialog is hidden by default`() {
        setDashboard()

        composeTestRule.onNodeWithText(string(R.string.upload_trees_soon_title)).assertDoesNotExist()
    }

    @Test
    fun `sync reminder dialog shows title and message`() {
        setDashboard(showSyncReminderDialog = true)

        composeTestRule.onNodeWithText(string(R.string.upload_trees_soon_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.upload_trees_text_content)).assertExists()
    }

    @Test
    fun `confirming the sync reminder dialog invokes onDialogConfirm`() {
        setDashboard(showSyncReminderDialog = true)

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        assertEquals(listOf(DIALOG_CONFIRM), clicks)
    }

    private companion object {
        const val SYNC = "sync"
        const val ORG = "org"
        const val CAPTURE = "capture"
        const val DIALOG_CONFIRM = "dialogConfirm"
        const val MESSAGES = "messages"
        const val SETTINGS = "settings"
    }
}