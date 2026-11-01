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
package org.greenstand.android.TreeTracker.uitest.messages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageItem
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the [IndividualMessageItem] card shown for each message in the inbox. */
class IndividualMessageItemUiTest : ComposeUiTest() {
    private var clicks = 0

    private fun render(
        isSelected: Boolean = false,
        isNotificationEnabled: Boolean = false,
        iconPadding: PaddingValues? = null,
    ) {
        setContent {
            IndividualMessageItem(
                isSelected = isSelected,
                isNotificationEnabled = isNotificationEnabled,
                messageTypeText = TYPE,
                text = TEXT,
                icon = R.drawable.individual_message_icon,
                iconPadding = iconPadding,
                onClick = { clicks++ },
            )
        }
    }

    @Test
    fun `item shows the message type in upper case and the message text`() {
        render()

        composeTestRule.onNodeWithText(TYPE.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPE).assertDoesNotExist()
    }

    @Test
    fun `clicking the text invokes onClick`() {
        render()

        composeTestRule.onNodeWithText(TEXT).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `clicking the header invokes onClick`() {
        render()

        composeTestRule.onNodeWithText(TYPE.uppercase()).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `a selected unread item with icon padding still shows its text and is clickable`() {
        render(isSelected = true, isNotificationEnabled = true, iconPadding = PaddingValues(bottom = 30.dp))

        composeTestRule.onNodeWithText(TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithText(TEXT).performClick()

        composeTestRule.runOnIdle { assertEquals(1, clicks) }
    }

    private companion object {
        const val TYPE = "Message"
        const val TEXT = "friend@example.com"
    }
}