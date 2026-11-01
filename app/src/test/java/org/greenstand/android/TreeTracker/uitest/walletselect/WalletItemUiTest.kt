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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.WalletItem
import org.greenstand.android.TreeTracker.walletselect.WalletSelect
import org.greenstand.android.TreeTracker.walletselect.WalletSelectAction
import org.greenstand.android.TreeTracker.walletselect.WalletSelectState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for a single [WalletItem] and how [WalletSelect] reacts to state changes. */
class WalletItemUiTest : ComposeUiTest() {
    @Test
    fun `wallet item shows the planter name wallet and tree count`() {
        setContent { WalletItem(user = walletTestOtherUser, isSelected = false, onClick = {}) }

        composeTestRule.onNodeWithText(OTHER_USER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(walletTestOtherUser.wallet).assertIsDisplayed()
        composeTestRule.onNodeWithText(walletTestOtherUser.numberOfTrees.toString()).assertIsDisplayed()
    }

    @Test
    fun `clicking a wallet item reports the planter id`() {
        val clickedIds = mutableListOf<Long>()
        setContent { WalletItem(user = walletTestOtherUser, isSelected = false, onClick = { clickedIds += it }) }

        composeTestRule.onNodeWithText(OTHER_USER_NAME).performClick()

        assertEquals(listOf(walletTestOtherUser.id), clickedIds)
    }

    @Test
    fun `a selected wallet item can still be clicked`() {
        val clickedIds = mutableListOf<Long>()
        setContent { WalletItem(user = walletTestCurrentUser, isSelected = true, onClick = { clickedIds += it }) }

        composeTestRule.onNodeWithText(CURRENT_USER_NAME).performClick()

        assertEquals(listOf(walletTestCurrentUser.id), clickedIds)
    }

    @Test
    fun `forward arrow starts working once a wallet gets selected`() {
        val actions = mutableListOf<WalletSelectAction>()
        var state by mutableStateOf(
            WalletSelectState(currentUser = walletTestCurrentUser, alternateUsers = listOf(walletTestOtherUser)),
        )
        setContent { WalletSelect(state = state, onHandleAction = { actions += it }) }
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.runOnIdle { assertEquals(emptyList<WalletSelectAction>(), actions) }

        state = state.copy(selectedUser = walletTestOtherUser)
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(WalletSelectAction.NavigateForward), actions) }
    }

    @Test
    fun `wallets added to the repository appear in the list`() {
        var state by mutableStateOf(WalletSelectState(currentUser = walletTestCurrentUser))
        setContent { WalletSelect(state = state) }
        composeTestRule.onNodeWithText(OTHER_USER_NAME).assertDoesNotExist()

        state = state.copy(alternateUsers = listOf(walletTestOtherUser))

        composeTestRule.onNodeWithText(OTHER_USER_NAME).assertIsDisplayed()
    }
}