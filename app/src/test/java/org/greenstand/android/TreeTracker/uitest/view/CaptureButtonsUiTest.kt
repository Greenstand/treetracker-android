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

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.CAPTURE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.CaptureButton
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.OrangeAddButton
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureButtonsUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `enabled capture button invokes onClick`() {
        setContent { CaptureButton(onClick = { clicks++ }, isEnabled = true) }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertIsDisplayed().performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `disabled capture button ignores clicks`() {
        setContent { CaptureButton(onClick = { clicks++ }, isEnabled = false) }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_TAG).assertIsDisplayed().performClick()

        composeTestRule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun `orange add button invokes onClick`() {
        setContent { OrangeAddButton(modifier = Modifier, onClick = { clicks++ }) }

        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).assertIsDisplayed().performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }
}