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
package org.greenstand.android.TreeTracker.uitest.userselect

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.userselect.UserSelect
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Tests for [UserSelect] (ViewModel + navigation wiring) and the [UserSelectScreen] root. */
class UserSelectScreenUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val preferences = mockk<Preferences>(relaxed = true)
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val forwardedUsers = mutableListOf<User>()

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.open() } just Runs
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { userRepo.users() } returns flowOf(userSelectTestUsers)
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun createViewModel(): UserSelectViewModel =
        UserSelectViewModel(
            userId = null,
            userRepo = userRepo,
            messageRepo = mockk<MessagesRepo>(relaxed = true),
            locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true),
            prefs = preferences,
        )

    private fun renderUserSelect(isFromSettings: Boolean = false) {
        val viewModel = createViewModel()
        setContent(inspectionMode = false) {
            UserSelect(
                navigationButtonColors = AppButtonColors.ProgressGreen,
                isCreateUserEnabled = true,
                isNotificationEnabled = true,
                isFromSettings = isFromSettings,
                viewModel = viewModel,
                onNavigateForward = { forwardedUsers += it },
            )
        }
    }

    @Test
    fun `users from the repository are shown`() {
        renderUserSelect()

        composeTestRule.onNodeWithText(ANN_NAME).assertExists()
        composeTestRule.onNodeWithText(BEN_NAME).assertExists()
    }

    @Test
    fun `selecting a user stores it and enables navigating forward with it`() {
        renderUserSelect()

        composeTestRule.onNodeWithText(BEN_NAME).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        verify { preferences.setUserId(userSelectTestUsers[1].id) }
        verify { captureSetupData.user = userSelectTestUsers[1] }
        assertEquals(listOf(userSelectTestUsers[1]), forwardedUsers)
    }

    @Test
    fun `forward arrow is inert before a user is selected`() {
        renderUserSelect()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(emptyList<User>(), forwardedUsers)
    }

    @Test
    fun `back arrow returns to a fresh dashboard`() {
        backStack.add(UserSelectRoute)
        renderUserSelect()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `back arrow pops one screen when opened from settings`() {
        backStack.add(SettingsRoute)
        backStack.add(TreeEditUserSelectRoute)
        renderUserSelect(isFromSettings = true)

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `add button opens the signup flow`() {
        renderUserSelect()

        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SignupFlowRoute), backStack.toList())
    }

    @Test
    fun `user select screen continues the capture setup flow with the selected user`() {
        val viewModel = createViewModel()
        setContent(inspectionMode = false) {
            UserSelectScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText(ANN_NAME).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify { setupNav.navForward(navigator) }
    }

    @Test
    fun `user select screen does not continue the setup flow without a selection`() {
        val viewModel = createViewModel()
        setContent(inspectionMode = false) {
            UserSelectScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 0) { setupNav.navForward(any()) }
    }
}