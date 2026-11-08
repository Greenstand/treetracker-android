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
package org.greenstand.android.TreeTracker.uitest.walletselect.addwallet

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWallet
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletAction
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletState
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the stateless [AddWallet] screen. */
class AddWalletUiTest : ComposeUiTest() {
    private val actions = mutableListOf<AddWalletAction>()

    private fun render(state: AddWalletState = AddWalletState()) {
        setContent {
            AddWallet(
                state = state,
                onHandleAction = { actions += it },
            )
        }
    }

    @Test
    fun `empty wallet name shows the placeholder`() {
        render()

        composeTestRule
            .onNodeWithText(string(R.string.name_placeholder), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `wallet name from state is shown in the field`() {
        render(state = AddWalletState(walletName = WALLET_NAME))

        composeTestRule.onNode(hasSetTextAction()).assertExists()
        composeTestRule.onNodeWithText(WALLET_NAME).assertExists()
    }

    @Test
    fun `typing updates the wallet name`() {
        render()

        composeTestRule.onNode(hasSetTextAction()).performTextInput(WALLET_NAME)

        assertEquals(listOf(AddWalletAction.UpdateWalletName(WALLET_NAME)), actions)
    }

    @Test
    fun `forward arrow is inert while the wallet name is blank`() {
        render(state = AddWalletState(walletName = "   "))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(emptyList<AddWalletAction>(), actions)
    }

    @Test
    fun `forward arrow navigates next once a wallet name is entered`() {
        render(state = AddWalletState(walletName = WALLET_NAME))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf(AddWalletAction.NavigateNext), actions)
    }

    @Test
    fun `keyboard go action navigates next`() {
        render(state = AddWalletState(walletName = WALLET_NAME))

        composeTestRule.onNode(hasSetTextAction()).performClick()
        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf(AddWalletAction.NavigateNext), actions)
    }

    @Test
    fun `back arrow navigates back`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf(AddWalletAction.NavigateBack), actions)
    }
}