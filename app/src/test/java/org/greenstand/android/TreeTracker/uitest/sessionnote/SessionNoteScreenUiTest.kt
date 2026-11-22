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
package org.greenstand.android.TreeTracker.uitest.sessionnote

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
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DoubleTapGuard
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Tests for the [SessionNoteScreen] root: ViewModel wiring and capture setup navigation. */
class SessionNoteScreenUiTest : ComposeUiTest() {
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
        every { captureSetupData.user } returns
            User(
                id = 3,
                wallet = "eve-wallet",
                numberOfTrees = 1,
                firstName = "Eve",
                lastName = "Njeri",
                photoPath = "/no/such/eve.jpg",
                isPowerUser = true,
                unreadMessagesAvailable = false,
            )
        val viewModel = SessionNoteViewModel()
        setContent(inspectionMode = false) {
            SessionNoteScreen(viewModel = viewModel, doubleTapGuard = doubleTapGuard)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `typing stores the session note and shows it`() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(SESSION_NOTE)
        composeTestRule.waitForIdle()

        verify { captureSetupData.sessionNote = SESSION_NOTE }
        composeTestRule.onNodeWithText(SESSION_NOTE).assertExists()
    }

    @Test
    fun `forward arrow continues the setup flow`() {
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify { setupNav.navForward(navigator) }
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
        navigator.navigate(SessionNoteRoute)

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 0) { setupNav.navForward(any()) }
    }

    @Test
    fun `forward arrow pressed a while after arriving continues the setup flow`() {
        navigator.navigate(SessionNoteRoute)
        // Each read moves the navigator's clock on a second: well past the guard's timeout.
        repeat(100) { navigator.millisSinceLastNavigation }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { setupNav.navForward(navigator) }
    }
}
