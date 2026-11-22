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

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavKey
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
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.uitest.walletselect.CURRENT_USER_NAME
import org.greenstand.android.TreeTracker.uitest.walletselect.walletTestCurrentUser
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Session note, wallet select and add wallet restored after process death (#1447): Navigation 3
 * brings the screen back while the capture setup scope, kept only in memory, is closed. Each
 * screen reopens the scope and reloads the setup user instead of crashing, holds Next and Back
 * until the user is back, and falls back to the Dashboard when the user can't be found.
 */
class SetupProcessDeathUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val preferences = mockk<Preferences>(relaxed = true)
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val restorer = CaptureSetupRestorer(preferences, userRepo)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        // The process died: nothing has opened the scope when the screen's view model is created.
        every { CaptureSetupScopeManager.isOpen } returns false
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { userRepo.users() } returns flowOf(listOf(walletTestCurrentUser))
        coEvery { userRepo.getUserList() } returns listOf(walletTestCurrentUser)
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun savedUserIsFound() {
        coEvery { userRepo.getUser(any()) } returns walletTestCurrentUser
    }

    private fun savedUserIsGone() {
        coEvery { userRepo.getUser(any()) } returns null
    }

    private fun savedUserIsStillLoading() {
        coEvery { userRepo.getUser(any()) } coAnswers { awaitCancellation() }
    }

    /** Shows [screen] on top of the back stack Navigation 3 restored: Dashboard, user select, [routes]. */
    private fun restore(
        vararg routes: NavKey,
        screen: @Composable () -> Unit,
    ) {
        backStack.add(UserSelectRoute)
        backStack.addAll(routes)
        setContent(inspectionMode = false, content = screen)
        composeTestRule.waitForIdle()
    }

    private fun restoreSessionNote() {
        // Built here, as the restored screen would build it: the view model reads the scope in init.
        val viewModel = SessionNoteViewModel(restorer)
        restore(SessionNoteRoute) { SessionNoteScreen(viewModel = viewModel) }
    }

    private fun restoreWalletSelect() {
        val viewModel = WalletSelectViewModel(userRepo, restorer)
        restore(WalletSelectRoute) { WalletSelectScreen(viewModel = viewModel) }
    }

    private fun restoreAddWallet() {
        val viewModel = AddWalletViewModel(restorer)
        restore(WalletSelectRoute, AddWalletRoute) { AddWalletScreen(viewModel = viewModel) }
    }

    private fun tap(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    // --- session note ---

    @Test
    fun `a restored session note reopens the scope and continues setup with the saved user`() {
        savedUserIsFound()
        restoreSessionNote()

        verify { CaptureSetupScopeManager.open() }
        verify { captureSetupData.user = walletTestCurrentUser }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("After the restore")
        tap(ARROW_FORWARD_BUTTON_TAG)

        verify { captureSetupData.sessionNote = "After the restore" }
        coVerify { setupNav.navForward(any()) }
    }

    @Test
    fun `Next and Back wait while a restored session note reloads its user`() {
        savedUserIsStillLoading()
        restoreSessionNote()

        tap(ARROW_FORWARD_BUTTON_TAG)
        tap(ARROW_BACK_BUTTON_TAG)

        coVerify(exactly = 0) { setupNav.navForward(any()) }
        verify(exactly = 0) { setupNav.navBackward(any()) }
    }

    @Test
    fun `a restored session note whose user is gone pops back to the Dashboard`() {
        savedUserIsGone()
        restoreSessionNote()

        assertEquals(listOf<NavKey>(DashboardRoute), backStack.toList())
    }

    // --- wallet select ---

    @Test
    fun `a restored wallet select shows the saved user and continues setup`() {
        savedUserIsFound()
        restoreWalletSelect()

        verify { CaptureSetupScopeManager.open() }
        verify { captureSetupData.user = walletTestCurrentUser }
        composeTestRule.onNodeWithText(CURRENT_USER_NAME).performClick()
        composeTestRule.waitForIdle()
        tap(ARROW_FORWARD_BUTTON_TAG)

        coVerify { setupNav.navForward(any()) }
    }

    @Test
    fun `a restored wallet select whose user is gone pops back to the Dashboard`() {
        savedUserIsGone()
        restoreWalletSelect()

        assertEquals(listOf<NavKey>(DashboardRoute), backStack.toList())
    }

    // --- add wallet ---

    @Test
    fun `a restored add wallet reopens the scope and continues setup with the saved user`() {
        savedUserIsFound()
        restoreAddWallet()

        verify { CaptureSetupScopeManager.open() }
        verify { captureSetupData.user = walletTestCurrentUser }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Tree Fund")
        tap(ARROW_FORWARD_BUTTON_TAG)

        verify { captureSetupData.destinationWallet = "Tree Fund" }
        coVerify { setupNav.navForward(any()) }
    }

    @Test
    fun `a restored add wallet whose user is gone pops back to the Dashboard`() {
        savedUserIsGone()
        restoreAddWallet()

        assertEquals(listOf<NavKey>(DashboardRoute), backStack.toList())
    }
}
