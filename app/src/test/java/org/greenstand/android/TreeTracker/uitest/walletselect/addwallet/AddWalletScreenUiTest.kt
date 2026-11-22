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
import androidx.compose.ui.test.performTextInput
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DoubleTapGuard
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.uitest.walletselect.walletTestCurrentUser
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Tests for the [AddWalletScreen] root: ViewModel wiring and capture setup navigation. */
class AddWalletScreenUiTest : ComposeUiTest() {
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)

    // A guard whose clock never moves and whose double-tap timeout is a minute, so only the
    // navigator's clock (one second per read, see ComposeUiTest) decides what it drops.
    private val doubleTapGuard = DoubleTapGuard(uptimeMillis = { 0L }, doubleTapTimeoutMs = { 60_000L })

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { captureSetupData.user } returns walletTestCurrentUser
        val viewModel = AddWalletViewModel()
        setContent(inspectionMode = false) {
            AddWalletScreen(viewModel = viewModel, doubleTapGuard = doubleTapGuard)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `typing stores the destination wallet and shows it`() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(WALLET_NAME)
        composeTestRule.waitForIdle()

        verify { captureSetupData.destinationWallet = WALLET_NAME }
        composeTestRule.onNodeWithText(WALLET_NAME).assertExists()
    }

    @Test
    fun `forward arrow continues the setup flow once a wallet is typed`() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(WALLET_NAME)
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify { setupNav.navForward(navigator) }
    }

    @Test
    fun `forward arrow does nothing before a wallet is typed`() {
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 0) { setupNav.navForward(any()) }
    }

    @Test
    fun `back arrow steps back in the setup flow`() {
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { setupNav.navBackward(navigator) }
    }

    // --- Second tap of a double tap on the previous screen's Next (#1442) ---

    @Test
    fun `forward arrow pressed right after arriving is dropped as the second tap of a double tap`() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(WALLET_NAME)
        navigator.navigate(AddWalletRoute)

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 0) { setupNav.navForward(any()) }
    }

    @Test
    fun `forward arrow pressed a while after arriving continues the setup flow`() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(WALLET_NAME)
        navigator.navigate(AddWalletRoute)
        // Each read moves the navigator's clock on a second: well past the guard's timeout.
        repeat(100) { navigator.millisSinceLastNavigation }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { setupNav.navForward(navigator) }
    }
}
