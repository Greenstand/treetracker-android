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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.USER_IMAGE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.UserImageButton
import org.junit.Assert.assertEquals
import org.junit.Test

class UserImageButtonUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `user image button is displayed`() {
        setContent {
            Box(Modifier.size(120.dp)) {
                UserImageButton(onClick = {}, imagePath = MISSING_IMAGE)
            }
        }

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `clicking the user image button invokes onClick`() {
        setContent {
            Box(Modifier.size(120.dp)) {
                UserImageButton(onClick = { clicks++ }, imagePath = MISSING_IMAGE)
            }
        }

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    private companion object {
        const val MISSING_IMAGE = "/does/not/exist.jpg"
    }
}