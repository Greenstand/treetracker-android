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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.Navigator
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.uitest.walletselect.OTHER_USER_NAME
import org.greenstand.android.TreeTracker.uitest.walletselect.walletTestCurrentUser
import org.greenstand.android.TreeTracker.uitest.walletselect.walletTestOtherUser
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
 * Next on the org-configured setup steps (wallet select, add wallet, session note) must drop the
 * second tap of a double tap that opened the screen, like user select and Add org do (#1423).
 *
 * Every setup screen's Next is the bottom bar's right arrow, so the second tap of a double tap on
 * the previous screen's Next lands on it. That tap is a first tap from a new screen on top, so
 * neither the origin throttle nor `CaptureSetupNavigationController.navForward`'s same-screen guard
 * stops it, and setup moves on a second step: the step is skipped, or capture opens.
 *
 * Each test renders the screen behind a navigator whose last navigation was [millisSinceOpened]
 * ago and counts how often Next reached `navForward`. No press is recorded on the navigator, so
 * the guard judges by the time since the navigation. How the guard judges a press itself (and that
 * each check uses up the press) is covered by `DoubleTapGuardTest`; these tests check that each
 * screen's Next and keyboard Go go through the guard.
 */
class SetupNextDoubleTapUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private var navForwardCalls = 0

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.isOpen } returns true
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { captureSetupData.user } returns walletTestCurrentUser
        coEvery { setupNav.navForward(any()) } answers { navForwardCalls++ }
        val users = listOf(walletTestCurrentUser, walletTestOtherUser)
        every { userRepo.users() } returns flowOf(users)
        coEvery { userRepo.getUserList() } returns users
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    /**
     * Shows [screen] as if the previous setup screen's Next pushed [route] [millisSinceOpened] ago:
     * the navigator's clock stands still at that distance from its last navigation.
     */
    private fun open(
        route: NavKey,
        millisSinceOpened: Long,
        screen: @Composable () -> Unit,
    ) {
        var nowMs = OPENED_AT_MS
        val setupNavigator = Navigator(backStack = mutableStateListOf(UserSelectRoute), clock = { nowMs })
        setupNavigator.navigate(route)
        nowMs = OPENED_AT_MS + millisSinceOpened
        setContent(inspectionMode = false) {
            CompositionLocalProvider(LocalNavigator provides setupNavigator) { screen() }
        }
    }

    private fun tapNext() {
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
    }

    private fun assertSecondTapDropped(screenName: String) {
        assertEquals(
            "Next on $screenName acted on the second tap of the double tap that opened it, " +
                "so capture setup moved on again and skipped a step",
            0,
            navForwardCalls,
        )
    }

    private fun assertTapAccepted(screenName: String) {
        assertEquals("Next on $screenName ignored a tap long after the screen opened", 1, navForwardCalls)
    }

    // --- session note: Next is always enabled ---

    private fun openSessionNote(millisSinceOpened: Long) {
        val viewModel = SessionNoteViewModel(CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
        open(SessionNoteRoute, millisSinceOpened) { SessionNoteScreen(viewModel = viewModel) }
    }

    @Test
    fun `session note Next drops the second tap of the double tap that opened it`() {
        openSessionNote(millisSinceOpened = 0)

        tapNext()

        assertSecondTapDropped("session note")
    }

    @Test
    fun `session note Next continues on a tap long after it opened`() {
        openSessionNote(millisSinceOpened = LONG_AFTER_MS)

        tapNext()

        assertTapAccepted("session note")
    }

    @Test
    fun `session note keyboard Go continues setup`() {
        openSessionNote(millisSinceOpened = LONG_AFTER_MS)

        composeTestRule.onNode(hasSetTextAction()).performImeAction()
        composeTestRule.waitForIdle()

        assertTapAccepted("session note")
    }

    // --- wallet select: Next is enabled once a wallet is picked ---

    private fun openWalletSelectAndPickWallet(millisSinceOpened: Long) {
        val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
        open(WalletSelectRoute, millisSinceOpened) { WalletSelectScreen(viewModel = viewModel) }
        composeTestRule.onNodeWithText(OTHER_USER_NAME).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `wallet select Next drops the second tap of the double tap that opened it`() {
        openWalletSelectAndPickWallet(millisSinceOpened = 0)

        tapNext()

        assertSecondTapDropped("wallet select")
    }

    @Test
    fun `wallet select Next continues on a tap long after it opened`() {
        openWalletSelectAndPickWallet(millisSinceOpened = LONG_AFTER_MS)

        tapNext()

        assertTapAccepted("wallet select")
    }

    // --- add wallet: Next is enabled once a wallet name is typed ---

    private fun openAddWalletAndTypeName(millisSinceOpened: Long) {
        val viewModel = AddWalletViewModel(CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
        open(AddWalletRoute, millisSinceOpened) { AddWalletScreen(viewModel = viewModel) }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Forest Fund")
        composeTestRule.waitForIdle()
    }

    @Test
    fun `add wallet Next drops the second tap of the double tap that opened it`() {
        openAddWalletAndTypeName(millisSinceOpened = 0)

        tapNext()

        assertSecondTapDropped("add wallet")
    }

    @Test
    fun `add wallet Next continues on a tap long after it opened`() {
        openAddWalletAndTypeName(millisSinceOpened = LONG_AFTER_MS)

        tapNext()

        assertTapAccepted("add wallet")
    }

    @Test
    fun `add wallet keyboard Go continues setup`() {
        openAddWalletAndTypeName(millisSinceOpened = LONG_AFTER_MS)

        composeTestRule.onNode(hasSetTextAction()).performImeAction()
        composeTestRule.waitForIdle()

        assertTapAccepted("add wallet")
    }

    private companion object {
        const val OPENED_AT_MS = 1_000_000L

        /** Far past any double-tap timeout, and past any gap between the test's and the press's clocks. */
        const val LONG_AFTER_MS = 1_000_000L
    }
}