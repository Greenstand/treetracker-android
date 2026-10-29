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

import androidx.compose.material.Text
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.RoundedImageContainer
import org.junit.Test

class ContainersUiTest : ComposeUiTest() {
    @Test
    fun `rounded image container renders its content`() {
        setContent {
            RoundedImageContainer {
                Text(CONTENT)
            }
        }

        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun `rounded image container is a 70dp square`() {
        setContent {
            RoundedImageContainer(modifier = Modifier.testTag(TAG)) {
                Text(CONTENT)
            }
        }

        composeTestRule
            .onNodeWithTag(TAG)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(70.dp)
    }

    private companion object {
        const val CONTENT = "AB"
        const val TAG = "container"
    }
}