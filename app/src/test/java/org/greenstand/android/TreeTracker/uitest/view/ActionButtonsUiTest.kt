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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ApprovalButton
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.INFO_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.InfoButton
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionButtonsUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `approval button renders the approve variant when approval is true`() {
        setContent { ApprovalButton(onClick = {}, approval = true) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `approval button renders the decline variant when approval is false`() {
        setContent { ApprovalButton(onClick = {}, approval = false) }

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `clicking the approve button invokes onClick`() {
        setContent { ApprovalButton(onClick = { clicks++ }, approval = true) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `clicking the decline button invokes onClick`() {
        setContent { ApprovalButton(onClick = { clicks++ }, approval = false) }

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `clicking the info button invokes onClick`() {
        setContent { InfoButton(onClick = { clicks++ }) }

        composeTestRule.onNodeWithTag(INFO_BUTTON_TAG).assertIsDisplayed().performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }
}