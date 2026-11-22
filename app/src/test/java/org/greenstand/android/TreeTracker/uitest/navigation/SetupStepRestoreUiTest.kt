/*
 * Copyright 2023 Treetracker
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

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
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
import org.greenstand.android.TreeTracker.uitest.walletselect.walletTestCurrentUser
import org.greenstand.android.TreeTracker.uitest.walletselect.walletTestOtherUser
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
 * Session note, wallet select and add wallet restored after process death (#1447): while the
 * setup user is reloaded the screen can't be left, and if that user is gone the screen falls back
 * to the Dashboard. The ViewModels are real; [CaptureSetupRestorer] stands in for the restore.
 */
class SetupStepRestoreUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val setupRestorer = mockk<CaptureSetupRestorer>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { captureSetupData.user } returns walletTestCurrentUser
        val users = listOf(walletTestCurrentUser, walletTestOtherUser)
        every { userRepo.users() } returns flowOf(users)
        coEvery { userRepo.getUserList() } returns users
        every { setupRestorer.reopenIfClosed() } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun stillReloadingUser() {
        coEvery { setupRestorer.reloadUser() } coAnswers { awaitCancellation() }
    }

    private fun userIsGone() {
        coEvery { setupRestorer.reloadUser() } returns false
    }

    private fun show(
        route: NavKey,
        restoredStack: List<NavKey> = listOf(DashboardRoute, UserSelectRoute, route),
    ) {
        backStack.clear()
        backStack.addAll(restoredStack)
        when (route) {
            SessionNoteRoute -> {
                val viewModel = SessionNoteViewModel(setupRestorer)
                setContent(inspectionMode = false) { SessionNoteScreen(viewModel = viewModel) }
            }
            WalletSelectRoute -> {
                val viewModel = WalletSelectViewModel(userRepo, setupRestorer)
                setContent(inspectionMode = false) { WalletSelectScreen(viewModel = viewModel) }
            }
            else -> {
                val viewModel = AddWalletViewModel(setupRestorer)
                setContent(inspectionMode = false) { AddWalletScreen(viewModel = viewModel) }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    private fun assertStayedOn(route: NavKey) {
        coVerify(exactly = 0) { setupNav.navForward(any()) }
        verify(exactly = 0) { setupNav.navBackward(any()) }
        assertEquals(listOf(DashboardRoute, UserSelectRoute, route), backStack.toList())
    }

    // --- Next and Back wait while the user is reloaded ---

    @Test
    fun `Next and Back wait while a restored session note reloads its user`() {
        stillReloadingUser()
        show(SessionNoteRoute)

        tap(ARROW_FORWARD_BUTTON_TAG)
        tap(ARROW_BACK_BUTTON_TAG)

        assertStayedOn(SessionNoteRoute)
    }

    @Test
    fun `Back and add wallet wait while a restored wallet select reloads its user`() {
        stillReloadingUser()
        show(WalletSelectRoute)

        tap(ARROW_BACK_BUTTON_TAG)
        tap(ORANGE_ADD_BUTTON_TAG)

        assertStayedOn(WalletSelectRoute)
    }

    @Test
    fun `Next and Back wait while a restored add wallet reloads its user`() {
        stillReloadingUser()
        show(AddWalletRoute)
        // Next is disabled until a wallet name is typed.
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Forest Fund")
        composeTestRule.waitForIdle()

        tap(ARROW_FORWARD_BUTTON_TAG)
        tap(ARROW_BACK_BUTTON_TAG)

        assertStayedOn(AddWalletRoute)
    }

    // --- Once the user is back the screen works as before ---

    @Test
    fun `Next continues setup from a restored session note once its user is back`() {
        coEvery { setupRestorer.reloadUser() } returns true
        show(SessionNoteRoute)

        tap(ARROW_FORWARD_BUTTON_TAG)

        coVerify(exactly = 1) { setupNav.navForward(navigator) }
    }

    // --- The user is gone: fall back to the Dashboard ---

    @Test
    fun `a restored session note whose user is gone pops back to the Dashboard`() {
        userIsGone()
        show(SessionNoteRoute)

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `a restored wallet select whose user is gone pops back to the Dashboard`() {
        userIsGone()
        show(WalletSelectRoute)

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `a restored add wallet whose user is gone pops back to the Dashboard`() {
        userIsGone()
        show(AddWalletRoute, restoredStack = listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddWalletRoute))

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `a restored session note whose user is gone opens the Dashboard when it is not on the stack`() {
        userIsGone()
        show(SessionNoteRoute, restoredStack = listOf(SessionNoteRoute))

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }
}
