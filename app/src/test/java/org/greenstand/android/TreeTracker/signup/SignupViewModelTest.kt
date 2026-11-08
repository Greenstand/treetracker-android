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
    private lateinit var signupViewModel: SignupViewModel

    @Before
    fun setupViewModel() {
        signupViewModel = SignupViewModel(userRepo, checkForInternetUseCase)
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

    // Initial state

    @Test
    fun `GIVEN no power user and internet WHEN created THEN selfie tutorial is shown and internet is available`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns true
            coEvery { userRepo.getPowerUser() } returns null

            val viewModel = SignupViewModel(userRepo, checkForInternetUseCase)

            val state = viewModel.state.value
            assertTrue(state.isInternetAvailable)
            assertEquals(true, state.showSelfieTutorial)
            assertEquals(false, state.isTherePowerUser)
        }

    @Test
    fun `GIVEN existing power user and no internet WHEN created THEN selfie tutorial is hidden`() =
        runTest {
            coEvery { checkForInternetUseCase.execute(Unit) } returns false
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.fakeUsers.first()

            val viewModel = SignupViewModel(userRepo, checkForInternetUseCase)

            val state = viewModel.state.value
            assertFalse(state.isInternetAvailable)
            assertEquals(false, state.showSelfieTutorial)
            assertEquals(true, state.isTherePowerUser)
        }

    @Test
    fun `WHEN created THEN starts on credential entry with an empty phone credential`() =
        runTest {
            val state = signupViewModel.state.value

            assertTrue(state.isCredentialView)
            assertFalse(state.isCredentialValid)
            assertFalse(state.canGoToNextScreen)
            assertFalse(state.autofocusTextEnabled)
            assertTrue(state.credential is Credential.Phone)
            assertEquals(true, state.showPrivacyDialog)
            assertNull(state.existingUser)
            assertNull(state.firstName)
            assertNull(state.lastName)
            assertNull(state.email)
            assertNull(state.phone)
            assertNull(state.firstNameError)
            assertNull(state.lastNameError)
            assertTrue(signupViewModel.events.replayCache.isEmpty())
        }

    // Name validation

    @Test
    fun `WHEN first name contains digits and symbols THEN they are filtered out without error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Ca1le#b"))

            assertEquals("Caleb", signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name is a single letter THEN shows minimum length error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("C"))

            assertEquals("C", signupViewModel.state.value.firstName)
            assertEquals("Name must be at least 2 characters", signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name is only whitespace THEN shows empty name error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("   "))

            assertEquals("   ", signupViewModel.state.value.firstName)
            assertEquals("Name cannot be empty", signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name is only digits THEN it is filtered to empty without error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("123"))

            assertEquals("", signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name is null THEN first name and error are cleared`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("C"))
            signupViewModel.handleAction(SignupAction.UpdateFirstName(null))

            assertNull(signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name is longer than 50 letters THEN it is truncated to 50 without error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("a".repeat(60)))

            assertEquals("a".repeat(50), signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name has apostrophes and hyphens THEN it is accepted unchanged`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("O'Brien-Smith"))

            assertEquals("O'Brien-Smith", signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN an invalid first name is corrected THEN the error is cleared`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("C"))
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Ca"))

            assertEquals("Ca", signupViewModel.state.value.firstName)
            assertNull(signupViewModel.state.value.firstNameError)
        }

    @Test
    fun `WHEN first name is updated THEN last name and its error are untouched`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName("K"))
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))

            assertEquals("K", signupViewModel.state.value.lastName)
            assertEquals("Name must be at least 2 characters", signupViewModel.state.value.lastNameError)
        }

    @Test
    fun `WHEN last name is a single letter THEN shows minimum length error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName("K"))

            assertEquals("K", signupViewModel.state.value.lastName)
            assertEquals("Name must be at least 2 characters", signupViewModel.state.value.lastNameError)
        }

    @Test
    fun `WHEN last name contains digits THEN they are filtered out without error`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName("Ka9leb"))

            assertEquals("Kaleb", signupViewModel.state.value.lastName)
            assertNull(signupViewModel.state.value.lastNameError)
        }

    @Test
    fun `WHEN last name is blank THEN no error is shown`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName("K"))
            signupViewModel.handleAction(SignupAction.UpdateLastName(""))

            assertEquals("", signupViewModel.state.value.lastName)
            assertNull(signupViewModel.state.value.lastNameError)
        }

    @Test
    fun `WHEN last name is null THEN last name and error are cleared`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateLastName("K"))
            signupViewModel.handleAction(SignupAction.UpdateLastName(null))

            assertNull(signupViewModel.state.value.lastName)
            assertNull(signupViewModel.state.value.lastNameError)
        }

    @Test
    fun `WHEN last name is updated THEN first name is untouched`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))
            signupViewModel.handleAction(SignupAction.UpdateLastName("Kaleb"))

            assertEquals("Caleb", signupViewModel.state.value.firstName)
        }

    @Test
    fun `WHEN no names entered THEN isFormValid returns false`() =
        runTest {
            assertFalse(signupViewModel.isFormValid())
        }

    @Test
    fun `WHEN last name is too short THEN isFormValid returns false`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))
            signupViewModel.handleAction(SignupAction.UpdateLastName("K"))

            assertFalse(signupViewModel.isFormValid())
        }

    // Email credential

    @Test
    fun `WHEN email is entered THEN it is lowercased and marked valid`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateEmail("John.Doe@Example.COM"))

            assertEquals("john.doe@example.com", signupViewModel.state.value.email)
            assertTrue(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `WHEN email has no at sign THEN credential is invalid`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateEmail("johndoe"))

            assertEquals("johndoe", signupViewModel.state.value.email)
            assertFalse(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `WHEN email is entered after a phone THEN the phone is cleared`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("0712345678"))
            signupViewModel.handleAction(SignupAction.UpdateEmail("john@example.com"))

            assertNull(signupViewModel.state.value.phone)
            assertEquals("john@example.com", signupViewModel.state.value.email)
        }

    // Phone credential

    @Test
    fun `WHEN phone is entered after an email THEN the email is cleared and validity follows the phone`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateEmail("john@example.com"))
            signupViewModel.handleAction(SignupAction.UpdatePhone("123"))

            assertNull(signupViewModel.state.value.email)
            assertEquals("123", signupViewModel.state.value.phone)
            assertFalse(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `WHEN phone has 6 digits THEN credential is invalid`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("123456"))

            assertFalse(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `WHEN phone has 7 digits THEN credential is valid`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("1234567"))

            assertTrue(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `WHEN phone has 15 digits THEN credential is valid`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("123456789012345"))

            assertTrue(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `WHEN phone has 16 digits THEN credential is invalid`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("1234567890123456"))

            assertFalse(signupViewModel.state.value.isCredentialValid)
        }

    // Credential type

    @Test
    fun `WHEN credential type switches back to phone THEN state credential is Phone`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Email()))
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Phone()))

            assertTrue(signupViewModel.state.value.credential is Credential.Phone)
        }

    @Test
    fun `WHEN credential type changes THEN entered phone and validity are kept`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("0712345678"))
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Email()))

            assertEquals("0712345678", signupViewModel.state.value.phone)
            assertTrue(signupViewModel.state.value.isCredentialValid)
        }

    @Test
    fun `Email credential validates its text with the email pattern`() {
        val credential = Credential.Email()
        assertFalse(credential.isValid)

        credential.text = "john@"
        assertFalse(credential.isValid)

        credential.text = "john@example.com"
        assertTrue(credential.isValid)
    }

    @Test
    fun `Phone credential validates its text with the phone pattern`() {
        val credential = Credential.Phone()
        assertFalse(credential.isValid)

        credential.text = "07123"
        assertFalse(credential.isValid)

        credential.text = "0712345678"
        assertTrue(credential.isValid)
    }

    // Submit info

    @Test
    fun `GIVEN email credential and existing user WHEN submitInfo THEN looks up by email and shows existing user`() =
        runTest {
            val existingUser = FakeFileGenerator.fakeUsers[1]
            coEvery { userRepo.doesUserExists("john@example.com") } returns true
            coEvery { userRepo.getUserWithWallet("john@example.com") } returns existingUser
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Email()))
            signupViewModel.handleAction(SignupAction.UpdateEmail("john@example.com"))

            signupViewModel.handleAction(SignupAction.SubmitInfo)

            assertEquals(existingUser, signupViewModel.state.value.existingUser)
            assertTrue(signupViewModel.state.value.isCredentialView)
        }

    @Test
    fun `GIVEN email credential and new user WHEN submitInfo THEN moves to name entry without user lookup`() =
        runTest {
            coEvery { userRepo.doesUserExists("john@example.com") } returns false
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Email()))
            signupViewModel.handleAction(SignupAction.UpdateEmail("john@example.com"))

            signupViewModel.handleAction(SignupAction.SubmitInfo)

            assertFalse(signupViewModel.state.value.isCredentialView)
            assertNull(signupViewModel.state.value.existingUser)
            coVerify(exactly = 0) { userRepo.getUserWithWallet(any()) }
        }

    @Test
    fun `GIVEN phone credential but only an email entered WHEN submitInfo THEN looks up the DEFAULT identifier`() =
        runTest {
            coEvery { userRepo.doesUserExists(any()) } returns false
            signupViewModel.handleAction(SignupAction.UpdateEmail("john@example.com"))

            signupViewModel.handleAction(SignupAction.SubmitInfo)

            coVerify { userRepo.doesUserExists("DEFAULT") }
        }

    @Test
    fun `WHEN existing user dialog is closed after a match THEN existing user is cleared`() =
        runTest {
            coEvery { userRepo.doesUserExists("0712345678") } returns true
            coEvery { userRepo.getUserWithWallet("0712345678") } returns FakeFileGenerator.fakeUsers.first()
            signupViewModel.handleAction(SignupAction.UpdatePhone("0712345678"))
            signupViewModel.handleAction(SignupAction.SubmitInfo)

            signupViewModel.handleAction(SignupAction.CloseExistingUserDialog)

            assertNull(signupViewModel.state.value.existingUser)
            assertTrue(signupViewModel.state.value.isCredentialView)
        }

    @Test
    fun `GIVEN name entry WHEN going back to credential entry THEN names are cleared and phone is kept`() =
        runTest {
            coEvery { userRepo.doesUserExists("0712345678") } returns false
            signupViewModel.handleAction(SignupAction.UpdatePhone("0712345678"))
            signupViewModel.handleAction(SignupAction.SubmitInfo)
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))
            signupViewModel.handleAction(SignupAction.UpdateLastName("Kaleb"))

            signupViewModel.handleAction(SignupAction.GoToCredentialEntry)

            val state = signupViewModel.state.value
            assertTrue(state.isCredentialView)
            assertTrue(state.canGoToNextScreen)
            assertNull(state.firstName)
            assertNull(state.lastName)
            assertEquals("0712345678", state.phone)
        }

    // Actions handled by the screen

    @Test
    fun `WHEN screen-handled actions are sent THEN state is unchanged and no events are emitted`() =
        runTest {
            signupViewModel.handleAction(SignupAction.UpdatePhone("0712345678"))
            val before = signupViewModel.state.value

            signupViewModel.handleAction(SignupAction.NavigateBack)
            signupViewModel.handleAction(SignupAction.LaunchCamera)
            signupViewModel.handleAction(SignupAction.ExistingUserSelected(FakeFileGenerator.fakeUsers.first()))

            assertEquals(before, signupViewModel.state.value)
            assertTrue(signupViewModel.events.replayCache.isEmpty())
            coVerify(exactly = 0) { userRepo.setPowerUserStatus(any(), any()) }
        }

    // Setup checks

    @Test
    fun `WHEN no power user exists THEN isInitialSetupRequired returns true`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null

            assertTrue(signupViewModel.isInitialSetupRequired())
        }

    @Test
    fun `WHEN a power user exists THEN isInitialSetupRequired returns false`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.fakeUsers.first()

            assertFalse(signupViewModel.isInitialSetupRequired())
        }

    // Create user

    @Test
    fun `WHEN createUser is called without a photo THEN returns null and creates nothing`() =
        runTest {
            val result = signupViewModel.createUser(null)

            assertNull(result)
            coVerify(exactly = 0) { userRepo.createUser(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN phone signup and no power user WHEN createUser THEN creates a power user with the phone as wallet`() =
        runTest {
            val createdUser = FakeFileGenerator.fakeUsers.first()
            coEvery { userRepo.getPowerUser() } returns null
            coEvery { userRepo.createUser(any(), any(), any(), any(), any(), any(), any()) } returns 42L
            coEvery { userRepo.getUser(42L) } returns createdUser
            signupViewModel.handleAction(SignupAction.UpdatePhone("0712345678"))
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Caleb"))
            signupViewModel.handleAction(SignupAction.UpdateLastName("Kaleb"))

            val result = signupViewModel.createUser("/photos/selfie.jpg")

            assertEquals(createdUser, result)
            coVerify {
                userRepo.createUser(
                    firstName = "Caleb",
                    lastName = "Kaleb",
                    phone = "0712345678",
                    email = null,
                    wallet = "0712345678",
                    photoPath = "/photos/selfie.jpg",
                    isPowerUser = true,
                )
            }
        }

    @Test
    fun `GIVEN email signup and an existing power user WHEN createUser THEN creates a regular user with the email as wallet`() =
        runTest {
            val createdUser = FakeFileGenerator.fakeUsers[1]
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.fakeUsers.first()
            coEvery { userRepo.createUser(any(), any(), any(), any(), any(), any(), any()) } returns 7L
            coEvery { userRepo.getUser(7L) } returns createdUser
            signupViewModel.handleAction(SignupAction.UpdateCredentialType(Credential.Email()))
            signupViewModel.handleAction(SignupAction.UpdateEmail("Jane@Example.com"))
            signupViewModel.handleAction(SignupAction.UpdateFirstName("Jane"))
            signupViewModel.handleAction(SignupAction.UpdateLastName("Joseph"))

            val result = signupViewModel.createUser("/photos/jane.jpg")

            assertEquals(createdUser, result)
            coVerify {
                userRepo.createUser(
                    firstName = "Jane",
                    lastName = "Joseph",
                    phone = null,
                    email = "jane@example.com",
                    wallet = "jane@example.com",
                    photoPath = "/photos/jane.jpg",
                    isPowerUser = false,
                )
            }
        }
}