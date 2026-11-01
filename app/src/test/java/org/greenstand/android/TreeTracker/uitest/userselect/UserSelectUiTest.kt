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
package org.greenstand.android.TreeTracker.uitest.userselect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.userselect.UserSelectContent
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the stateless [UserSelectContent] grid. */
class UserSelectUiTest : ComposeUiTest() {
    private val clickedUsers = mutableListOf<User>()
    private val forwardedUsers = mutableListOf<User>()
    private var createUserClicks = 0
    private var backClicks = 0

    private fun render(
        state: UserSelectState = UserSelectState(users = userSelectTestUsers),
        isCreateUserEnabled: Boolean = true,
    ) {
        setContent {
            UserSelectContent(
                state = state,
                navigationButtonColors = AppButtonColors.ProgressGreen,
                isCreateUserEnabled = isCreateUserEnabled,
                isNotificationEnabled = true,
                onUserClicked = { clickedUsers += it },
                onCreateUserClicked = { createUserClicks++ },
                onBackClicked = { backClicks++ },
                onNavigateForward = { forwardedUsers += it },
            )
        }
    }

    @Test
    fun `every user is shown with name wallet and tree count`() {
        render()

        userSelectTestUsers.forEach { user ->
            composeTestRule.onNodeWithText("${user.firstName} ${user.lastName}").assertIsDisplayed()
            composeTestRule.onNodeWithText(user.wallet).assertIsDisplayed()
            composeTestRule.onNodeWithText(user.numberOfTrees.toString()).assertIsDisplayed()
        }
    }

    @Test
    fun `empty user list still shows the navigation buttons`() {
        render(state = UserSelectState())

        composeTestRule.onNodeWithText(ANN_NAME).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `clicking a user reports that user`() {
        render()

        composeTestRule.onNodeWithText(BEN_NAME).performClick()

        assertEquals(listOf(userSelectTestUsers[1]), clickedUsers)
    }

    @Test
    fun `forward arrow does nothing while no user is selected`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertTrue(forwardedUsers.isEmpty())
    }

    @Test
    fun `forward arrow navigates forward with the selected user`() {
        render(state = UserSelectState(users = userSelectTestUsers, selectedUser = userSelectTestUsers[0]))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf(userSelectTestUsers[0]), forwardedUsers)
    }

    @Test
    fun `back arrow reports a back click`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `add button reports a create user click`() {
        render()

        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).performClick()

        assertEquals(1, createUserClicks)
    }

    @Test
    fun `add button is hidden when creating users is disabled`() {
        render(isCreateUserEnabled = false)

        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).assertDoesNotExist()
    }
}