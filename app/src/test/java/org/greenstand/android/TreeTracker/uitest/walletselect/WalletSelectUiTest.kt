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
package org.greenstand.android.TreeTracker.uitest.walletselect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.USER_IMAGE_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.WalletSelect
import org.greenstand.android.TreeTracker.walletselect.WalletSelectAction
import org.greenstand.android.TreeTracker.walletselect.WalletSelectState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the stateless [WalletSelect] screen. */
class WalletSelectUiTest : ComposeUiTest() {
    private val actions = mutableListOf<WalletSelectAction>()

    private val filledState =
        WalletSelectState(
            currentUser = walletTestCurrentUser,
            alternateUsers = listOf(walletTestOtherUser),
        )

    private fun render(state: WalletSelectState = filledState) {
        setContent {
            WalletSelect(
                state = state,
                onHandleAction = { actions += it },
            )
        }
    }

    @Test
    fun `current user and alternate wallets are listed`() {
        render()

        composeTestRule.onNodeWithText(CURRENT_USER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(walletTestCurrentUser.wallet).assertIsDisplayed()
        composeTestRule.onNodeWithText(OTHER_USER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(walletTestOtherUser.wallet).assertIsDisplayed()
    }

    @Test
    fun `profile button is shown for the current user`() {
        render()

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `empty state shows no wallets and no profile button`() {
        render(state = WalletSelectState())

        composeTestRule.onNodeWithText(CURRENT_USER_NAME).assertDoesNotExist()
        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `clicking a wallet selects that planter`() {
        render()

        composeTestRule.onNodeWithText(OTHER_USER_NAME).performClick()

        assertEquals(listOf(WalletSelectAction.SelectPlanter(walletTestOtherUser.id)), actions)
    }

    @Test
    fun `clicking the current user wallet selects the current user`() {
        render()

        composeTestRule.onNodeWithText(CURRENT_USER_NAME).performClick()

        assertEquals(listOf(WalletSelectAction.SelectPlanter(walletTestCurrentUser.id)), actions)
    }

    @Test
    fun `profile button navigates to user select`() {
        render()

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).performClick()

        assertEquals(listOf(WalletSelectAction.NavigateToUserSelect), actions)
    }

    @Test
    fun `forward arrow is inert until a wallet is selected`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(emptyList<WalletSelectAction>(), actions)
    }

    @Test
    fun `forward arrow navigates forward once a wallet is selected`() {
        render(state = filledState.copy(selectedUser = walletTestOtherUser))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf(WalletSelectAction.NavigateForward), actions)
    }

    @Test
    fun `add button navigates to add wallet`() {
        render()

        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).performClick()

        assertEquals(listOf(WalletSelectAction.NavigateToAddWallet), actions)
    }

    @Test
    fun `back arrow navigates back`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf(WalletSelectAction.NavigateBack), actions)
    }
}