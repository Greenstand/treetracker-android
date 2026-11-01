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
package org.greenstand.android.TreeTracker.uitest.view.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.NoGPSDeviceDialog
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsDialogUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `no gps dialog explains that the device has no gps`() {
        setContent { NoGPSDeviceDialog(onPositiveClick = {}) }

        composeTestRule.onNodeWithText(string(R.string.no_gps_device_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.no_gps_device_content)).assertIsDisplayed()
    }

    @Test
    fun `no gps dialog only offers the approve button`() {
        setContent { NoGPSDeviceDialog(onPositiveClick = {}) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `clicking approve invokes onPositiveClick`() {
        setContent { NoGPSDeviceDialog(onPositiveClick = { clicks++ }) }

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }
}