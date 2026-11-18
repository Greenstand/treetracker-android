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
package org.greenstand.android.TreeTracker.uitest.navigation

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Setup steps after user select, restored after process death: they wait while their user
 * reloads, and leave for the Dashboard when that user is gone.
 */
class CaptureSetupRestoreUiTest : ComposeUiTest() {
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val restorer = CaptureSetupRestorer(mockk(relaxed = true), userRepo)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.isOpen } returns false
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun userStillReloading() {
        coEvery { userRepo.getUser(any()) } coAnswers { awaitCancellation() }
    }

    private fun userIsGone() {
        coEvery { userRepo.getUser(any()) } returns null
    }

    private fun clickAndSettle(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `a restored session note waits for its user before moving on`() {
        userStillReloading()
        SessionNoteViewModel(restorer).let { setContent(inspectionMode = false) { SessionNoteScreen(viewModel = it) } }

        clickAndSettle(ARROW_FORWARD_BUTTON_TAG)
        clickAndSettle(ARROW_BACK_BUTTON_TAG)

        coVerify(exactly = 0) { setupNav.navForward(any()) }
        verify(exactly = 0) { setupNav.navBackward(any()) }
    }

    @Test
    fun `a restored wallet select waits for its user before moving on`() {
        userStillReloading()
        WalletSelectViewModel(userRepo, restorer).let { setContent(inspectionMode = false) { WalletSelectScreen(viewModel = it) } }

        clickAndSettle(ORANGE_ADD_BUTTON_TAG)
        clickAndSettle(ARROW_BACK_BUTTON_TAG)

        assertEquals(listOf(DashboardRoute), backStack.toList())
        verify(exactly = 0) { setupNav.navBackward(any()) }
    }

    @Test
    fun `a restored add wallet waits for its user before going back`() {
        userStillReloading()
        AddWalletViewModel(restorer).let { setContent(inspectionMode = false) { AddWalletScreen(viewModel = it) } }

        clickAndSettle(ARROW_BACK_BUTTON_TAG)

        verify(exactly = 0) { setupNav.navBackward(any()) }
    }

    @Test
    fun `a restored session note whose user is gone ends on the Dashboard`() {
        userIsGone()
        backStack.addAll(listOf(UserSelectRoute, SessionNoteRoute))
        SessionNoteViewModel(restorer).let { setContent(inspectionMode = false) { SessionNoteScreen(viewModel = it) } }
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `a restored wallet select whose user is gone ends on the Dashboard`() {
        userIsGone()
        backStack.addAll(listOf(UserSelectRoute, WalletSelectRoute))
        WalletSelectViewModel(userRepo, restorer).let { setContent(inspectionMode = false) { WalletSelectScreen(viewModel = it) } }
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `a restored add wallet whose user is gone opens the Dashboard when it is not on the stack`() {
        userIsGone()
        backStack.clear()
        backStack.add(AddWalletRoute)
        AddWalletViewModel(restorer).let { setContent(inspectionMode = false) { AddWalletScreen(viewModel = it) } }
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }
}