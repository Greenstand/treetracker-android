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

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ArrowButton
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationButtonsUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `left arrow button renders as the back arrow`() {
        setContent { Box { ArrowButton(isLeft = true, onClick = {}) } }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `right arrow button renders as the forward arrow`() {
        setContent { Box { ArrowButton(isLeft = false, onClick = {}) } }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `enabled arrow button invokes onClick`() {
        setContent { Box { ArrowButton(isLeft = false, onClick = { clicks++ }) } }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `disabled arrow button ignores clicks`() {
        setContent { Box { ArrowButton(isEnabled = false, isLeft = false, onClick = { clicks++ }) } }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(0, clicks) }
    }
}