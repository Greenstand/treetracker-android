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
package org.greenstand.android.TreeTracker.uitest.signup

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.signup.SignUpScreen
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignUpScreenUiTest : RootScreenUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>()
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val captureSetupNav = mockk<CaptureSetupNavigationController>(relaxed = true)

    private val existingUser =
        User(
            id = 7L,
            wallet = PHONE,
            numberOfTrees = 3,
            firstName = "Jane",
            lastName = "Doe",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false,
        )

    private val powerUser = existingUser.copy(id = 1L, wallet = "power@user.org", isPowerUser = true)

    @Before
    fun setUp() {
        coEvery { checkForInternetUseCase.execute(Unit) } returns false
        coEvery { userRepo.doesUserExists(PHONE) } returns true
        coEvery { userRepo.getUserWithWallet(PHONE) } returns existingUser
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns captureSetupNav
        startKoinWith(languageButtonModule())
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun launchSignUp(powerUserOnDevice: User?): SignupViewModel {
        coEvery { userRepo.getPowerUser() } returns powerUserOnDevice
        val viewModel = SignupViewModel(userRepo, checkForInternetUseCase)
        setRootContent { SignUpScreen(viewModel = viewModel) }
        composeTestRule.waitUntil { viewModel.state.value.isTherePowerUser != null }
        return viewModel
    }

    private fun closePrivacyDialog() {
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertDoesNotExist()
    }

    private fun submitExistingUserPhone() {
        composeTestRule.onNode(hasSetTextAction()).performTextInput(PHONE)
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.user_exists_header)).assertExists()
    }

    @Test
    fun `accepting the privacy policy reveals the navigation buttons`() {
        launchSignUp(powerUserOnDevice = null)

        closePrivacyDialog()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertExists()
    }

    @Test
    fun `back pops the sign up screen`() {
        backStack.add(SignupFlowRoute)
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `submitting a known phone shows the existing user dialog`() {
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()

        submitExistingUserPhone()

        composeTestRule.onNodeWithText("Jane Doe").assertExists()
    }

    @Test
    fun `existing user picked on first setup becomes power user and opens dashboard`() {
        backStack.clear()
        backStack.add(LanguageRoute(isFromTopBar = false))
        backStack.add(SignupFlowRoute)
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()
        submitExistingUserPhone()

        composeTestRule.onNodeWithText("Jane Doe").performClick()
        composeTestRule.waitForIdle()

        coVerify { userRepo.setPowerUserStatus(existingUser.id, true) }
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `existing user picked with a power user continues the capture setup flow`() {
        backStack.add(SignupFlowRoute)
        launchSignUp(powerUserOnDevice = powerUser)
        closePrivacyDialog()
        submitExistingUserPhone()

        composeTestRule.onNodeWithText("Jane Doe").performClick()
        composeTestRule.waitForIdle()

        verify { captureSetupData.user = existingUser }
        coVerify { captureSetupNav.navFromNewUserCreation(navigator) }
        coVerify(exactly = 0) { userRepo.setPowerUserStatus(any(), any()) }
    }

    @Test
    fun `submitting an unknown phone moves on to the name entry`() {
        coEvery { userRepo.doesUserExists(NEW_PHONE) } returns false
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()

        composeTestRule.onNode(hasSetTextAction()).performTextInput(NEW_PHONE)
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.last_name_hint)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.user_exists_header)).assertDoesNotExist()
    }

    @Test
    fun `an invalid phone shows the phone error and is not looked up`() {
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("12")
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.phone_validation_error)).assertExists()
        coVerify(exactly = 0) { userRepo.doesUserExists(any()) }
    }

    @Test
    fun `an email is lower cased and looked up by that email`() {
        coEvery { userRepo.doesUserExists(EMAIL) } returns false
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()
        composeTestRule.onNodeWithText(string(R.string.email_placeholder).uppercase()).performClick()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Jane@Example.ORG")
        composeTestRule.onNode(hasSetTextAction() and hasText(EMAIL)).assertExists()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        coVerify { userRepo.doesUserExists(EMAIL) }
        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertExists()
    }

    @Test
    fun `an email without an at sign shows the email error`() {
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()
        composeTestRule.onNodeWithText(string(R.string.email_placeholder).uppercase()).performClick()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("jane.example.org")
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.email_validation_error)).assertExists()
        coVerify(exactly = 0) { userRepo.doesUserExists(any()) }
    }

    @Test
    fun `declining the existing user dialog closes it and keeps the credential entry`() {
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()
        submitExistingUserPhone()

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.user_exists_header)).assertDoesNotExist()
        composeTestRule.onNode(hasSetTextAction() and hasText(PHONE)).assertExists()
        coVerify(exactly = 0) { userRepo.setPowerUserStatus(any(), any()) }
    }

    @Test
    fun `live web map link is shown when the internet is reachable`() {
        coEvery { checkForInternetUseCase.execute(Unit) } returns true
        launchSignUp(powerUserOnDevice = null)
        closePrivacyDialog()

        composeTestRule.onNodeWithText(string(R.string.viewLiveWebMap)).assertExists()
    }

    @Test
    fun `next on the name entry is enabled only once both names are valid`() {
        val viewModel = launchSignUp(powerUserOnDevice = null)
        goToNameEntry()

        nameField(0).performTextInput("Jane")
        assertFalse(viewModel.isFormValid())
        nameField(1).performTextInput("Doe")

        composeTestRule.waitForIdle()
        assertTrue(viewModel.isFormValid())
    }

    @Test
    fun `digits and symbols typed into a name are filtered out`() {
        val viewModel = launchSignUp(powerUserOnDevice = null)
        goToNameEntry()

        nameField(0).performTextInput("Ja3ne!")

        composeTestRule.onNode(hasSetTextAction() and hasText("Jane")).assertExists()
        assertEquals("Jane", viewModel.state.value.firstName)
    }

    @Test
    fun `a one letter name shows the length error`() {
        launchSignUp(powerUserOnDevice = null)
        goToNameEntry()

        nameField(1).performTextInput("D")

        composeTestRule.onNodeWithText("Name must be at least 2 characters").assertExists()
    }

    @Test
    fun `back on the name entry returns to the credential entry without the privacy policy`() {
        launchSignUp(powerUserOnDevice = null)
        goToNameEntry()
        nameField(0).performTextInput("Jane")

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.phone_placeholder).uppercase()).assertExists()
        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertDoesNotExist()
    }

    private fun goToNameEntry() {
        coEvery { userRepo.doesUserExists(NEW_PHONE) } returns false
        closePrivacyDialog()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(NEW_PHONE)
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertExists()
    }

    private fun nameField(index: Int) = composeTestRule.onAllNodes(hasSetTextAction())[index]

    private companion object {
        const val PHONE = "0712345678"
        const val NEW_PHONE = "0798765432"
        const val EMAIL = "jane@example.org"
    }
}