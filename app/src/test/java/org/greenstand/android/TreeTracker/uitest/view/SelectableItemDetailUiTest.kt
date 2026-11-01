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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.SelectableImageDetail
import org.greenstand.android.TreeTracker.view.UserButton
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectableItemDetailUiTest : ComposeUiTest() {
    private var clicks = 0

    @Test
    fun `selectable detail renders its header and content`() {
        setContent {
            SelectableImageDetail(
                isSelected = false,
                buttonColors = AppButtonColors.Default,
                selectedColor = AppColors.Green,
                onClick = {},
                header = { Text(HEADER) },
            ) {
                Text(CONTENT)
            }
        }

        composeTestRule.onNodeWithText(HEADER).assertIsDisplayed()
        composeTestRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun `selectable detail with a placeholder still renders its content`() {
        setContent {
            SelectableImageDetail(
                isSelected = true,
                buttonColors = AppButtonColors.Default,
                selectedColor = AppColors.Green,
                onClick = {},
                placeholderResId = R.drawable.tree_capturing_illustration,
            ) {
                Text(CONTENT)
            }
        }

        composeTestRule.onNodeWithText(CONTENT).assertExists()
    }

    @Test
    fun `clicking an unselected detail invokes onClick`() {
        setContent {
            SelectableImageDetail(
                isSelected = false,
                buttonColors = AppButtonColors.Default,
                selectedColor = AppColors.Green,
                onClick = { clicks++ },
            ) {
                Text(CONTENT)
            }
        }

        composeTestRule.onNodeWithText(CONTENT).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `clicking a selected detail invokes onClick`() {
        setContent {
            SelectableImageDetail(
                isSelected = true,
                buttonColors = AppButtonColors.Default,
                selectedColor = AppColors.Green,
                onClick = { clicks++ },
            ) {
                Text(CONTENT)
            }
        }

        composeTestRule.onNodeWithText(CONTENT).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `user button shows the name, wallet and tree count`() {
        setContent {
            UserButton(
                user = USER,
                isSelected = false,
                buttonColors = AppButtonColors.Default,
                selectedColor = AppColors.Green,
                onClick = {},
            )
        }

        composeTestRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeTestRule.onNodeWithText("ada@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun `clicking a user button invokes onClick`() {
        setContent {
            UserButton(
                user = USER,
                isSelected = false,
                buttonColors = AppButtonColors.Default,
                selectedColor = AppColors.Green,
                onClick = { clicks++ },
                isNotificationEnabled = true,
            )
        }

        composeTestRule.onNodeWithText("Ada Lovelace").performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    private companion object {
        const val HEADER = "Header"
        const val CONTENT = "Content"

        val USER =
            User(
                id = 1,
                wallet = "ada@example.com",
                numberOfTrees = 12,
                firstName = "Ada",
                lastName = "Lovelace",
                photoPath = "/does/not/exist.jpg",
                isPowerUser = false,
                unreadMessagesAvailable = true,
            )
    }
}