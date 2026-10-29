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

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.USER_IMAGE_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Tests for the [WalletSelectScreen] root: ViewModel wiring and capture setup navigation. */
class WalletSelectScreenUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { captureSetupData.user } returns walletTestCurrentUser
        val users = listOf(walletTestCurrentUser, walletTestOtherUser)
        every { userRepo.users() } returns flowOf(users)
        coEvery { userRepo.getUserList() } returns users
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun render() {
        val viewModel = WalletSelectViewModel(userRepo)
        setContent(inspectionMode = false) {
            WalletSelectScreen(viewModel = viewModel)
        }
    }

    @Test
    fun `current user and the other planters come from the setup data and repository`() {
        render()

        composeTestRule.onNodeWithText(CURRENT_USER_NAME).assertExists()
        composeTestRule.onNodeWithText(OTHER_USER_NAME).assertExists()
        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).assertExists()
    }

    @Test
    fun `selecting a wallet stores it as the destination and continues the setup flow`() {
        render()

        composeTestRule.onNodeWithText(OTHER_USER_NAME).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { captureSetupData.destinationWallet = walletTestOtherUser.wallet }
        coVerify { setupNav.navForward(navigator) }
    }

    @Test
    fun `forward arrow does not continue the setup flow without a selection`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 0) { setupNav.navForward(any()) }
    }

    @Test
    fun `add button opens add wallet`() {
        render()

        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, AddWalletRoute), backStack.toList())
    }

    @Test
    fun `back arrow steps back in the setup flow`() {
        render()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { setupNav.navBackward(navigator) }
    }

    @Test
    fun `profile button returns to user select`() {
        render()

        composeTestRule.onNodeWithTag(USER_IMAGE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { setupNav.navToUserSelect(navigator) }
    }
}