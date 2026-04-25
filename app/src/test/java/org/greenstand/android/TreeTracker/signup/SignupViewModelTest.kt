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
package org.greenstand.android.TreeTracker.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SignupViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>(relaxed = true)
    private val preferences = mockk<Preferences>(relaxed = true)
    private lateinit var signupViewModel: SignupViewModel

    @Before
    fun setupViewModel() {
        signupViewModel = SignupViewModel(userRepo, checkForInternetUseCase, preferences)
    }

    // First Name
    @Test
    fun `update first name, returns valid first name`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))
            assertEquals(signupViewModel.state.value.firstName, FakeFileGenerator.fakeUsers.first().firstName)
        }

    @Test
    fun `Blank first name, returns Empty input `() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName(""))
            assertEquals(signupViewModel.state.value.firstName, FakeFileGenerator.emptyUser.firstName)
        }

    // Last Name
    @Test
    fun `update last name, returns valid last name`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName("Kaleb"))
            assertEquals(signupViewModel.state.value.lastName, FakeFileGenerator.fakeUsers.first().lastName)
        }

    @Test
    fun `Blank last name, returns Empty input `() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName(""))
            assertEquals(signupViewModel.state.value.lastName, FakeFileGenerator.emptyUser.lastName)
        }

    // Email
    @Test
    fun `update email contains valid credentials, returns success`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateEmail("somerandom@gmsail.com"))
            assertNull(signupViewModel.state.value.phone)
            assertEquals(signupViewModel.state.value.email, "somerandom@gmsail.com")
        }

    @Test
    fun `update email contains invalid credentials, returns Error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateEmail("somerandom!!gmsail.com"))
            assertNotEquals(signupViewModel.state.value.email, "somerandom@gmsail.com")
        }

    // Phone
    @Test
    fun `update phone contains valid credentials, returns success`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("071321998893"))
            assertTrue(signupViewModel.state.value.isCredentialValid)
            assertNull(signupViewModel.state.value.email)
        }

    @Test
    fun `update phone contains invalid credentials, returns Error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("random1919"))
            assertFalse(signupViewModel.state.value.isCredentialValid)
        }

    // Selfie Tutorial Dialogs
    @Test
    fun `update selfie tutorial dialog state = false, returns false`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateSelfieTutorialDialog(false))
            assertFalse(signupViewModel.state.value.showSelfieTutorial!!)
        }

    @Test
    fun `update selfie tutorial dialog state = true, returns true`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateSelfieTutorialDialog(true))
            assertTrue(signupViewModel.state.value.showSelfieTutorial!!)
        }

    // Go to Credentials Entry
    @Test
    fun `Go to credentials Entry, returns true`() =
        runTest {
            signupViewModel.handleAction(SignupAction.GoToCredentialEntry)
            assertTrue(signupViewModel.state.value.isCredentialView)
            assertTrue(signupViewModel.state.value.canGoToNextScreen)
            assertNull(signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.lastName)
        }

    // Enables Autofocus
    @Test
    fun `enable autofocus, returns true`() =
        runTest {
            signupViewModel.handleAction(SignupAction.EnableAutofocus)
            assertTrue(signupViewModel.state.value.autofocusTextEnabled)
        }

    // Close privacy policy dialog
    @Test
    fun `close privacy policy dialog, returns false`() =
        runTest {
            signupViewModel.handleAction(SignupAction.ClosePrivacyPolicyDialog)
            assertFalse(signupViewModel.state.value.showPrivacyDialog!!)
        }

    // Close Existing User Dialog
    @Test
    fun `close existing user dialog, returns true`() =
        runTest {
            signupViewModel.handleAction(SignupAction.CloseExistingUserDialog)
            assertNull(signupViewModel.state.value.existingUser)
        }

    @Test
    fun `WHEN form has valid names THEN isFormValid returns true`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))
            signupViewModel.handleAction(SignupAction.UpdateLastName("Kaleb"))
            assertTrue(signupViewModel.isFormValid())
        }

    @Test
    fun `WHEN form has empty first name THEN isFormValid returns false`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName(""))
            signupViewModel.handleAction(SignupAction.UpdateLastName("Kaleb"))
            assertFalse(signupViewModel.isFormValid())
        }

    @Test
    fun `WHEN updateCredentialType called with Email THEN state credential is Email`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Email()))
            assertTrue(signupViewModel.state.value.credential is Credential.Email)
        }

    @Test
    fun `WHEN submitInfo and user exists THEN existingUser is set`() =
        runTest {
            val existingUser = FakeFileGenerator.fakeUsers.first()
            signupViewModel.handleAction(SignupAction.UpdatePhone("071321998893"))

            coEvery { userRepo.doesUserExists("071321998893") } returns true
            coEvery { userRepo.getUserWithWallet("071321998893") } returns existingUser

            signupViewModel.handleAction(SignupAction.SubmitInfo)

            assertNotNull(signupViewModel.state.value.existingUser)
            assertEquals(
                existingUser.id,
                signupViewModel.state.value.existingUser!!
                    .id,
            )
        }

    @Test
    fun `WHEN submitInfo and user not exists THEN navigates to name entry`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("071321998893"))

            coEvery { userRepo.doesUserExists("071321998893") } returns false

            signupViewModel.handleAction(SignupAction.SubmitInfo)

            assertFalse(signupViewModel.state.value.isCredentialView)
            assertFalse(signupViewModel.state.value.canGoToNextScreen)
        }

    @Test
    fun `WHEN setExistingUserAsPowerUser THEN calls setPowerUserStatus`() =
        runTest {
            val userId = 122L
            signupViewModel.handleAction(SignupAction.SetExistingUserAsPowerUser(userId))

            coVerify { userRepo.setPowerUserStatus(userId, true) }
        }
}