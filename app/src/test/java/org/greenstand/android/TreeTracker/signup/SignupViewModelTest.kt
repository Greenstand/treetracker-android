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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
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
    fun `update first name, returns valid first name`() = runBlocking {
        signupViewModel.updateFirstName("Caleb")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().firstName, FakeFileGenerator.fakeUsers.first().firstName)
    }
    @Test
    fun `Blank first name, returns Empty input `() = runBlocking {
        signupViewModel.updateFirstName("")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().firstName, FakeFileGenerator.emptyUser.firstName)
    }
    // Last Name
    @Test
    fun `update last name, returns valid last name`() = runBlocking {
        signupViewModel.updateLastName("Kaleb")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().lastName, FakeFileGenerator.fakeUsers.first().lastName)
    }
    @Test
    fun `Blank last name, returns Empty input `() = runBlocking {
        signupViewModel.updateLastName("")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().lastName, FakeFileGenerator.emptyUser.lastName)
    }
    // Email
    @Test
    fun `update email contains valid credentials, returns success`() = runBlocking {
        signupViewModel.updateEmail("somerandom@gmsail.com")
        assertNull(signupViewModel.state.getOrAwaitValueTest().phone)
        assertEquals(signupViewModel.state.getOrAwaitValueTest().email, "somerandom@gmsail.com")
    }
    @Test
    fun `update email contains invalid credentials, returns Error`() = runBlocking {
        signupViewModel.updateEmail("somerandom!!gmsail.com")
        assertNotEquals(signupViewModel.state.getOrAwaitValueTest().email, "somerandom@gmsail.com")
    }
    // Phone
    @Test
    fun `update phone contains valid credentials, returns success`() = runBlocking {
        signupViewModel.updatePhone("071321998893")
        assertTrue(signupViewModel.state.getOrAwaitValueTest().isCredentialValid)
        assertNull(signupViewModel.state.getOrAwaitValueTest().email)
    }
    @Test
    fun `update phone contains invalid credentials, returns Error`() = runBlocking {
        signupViewModel.updatePhone("random1919")
        assertFalse(signupViewModel.state.getOrAwaitValueTest().isCredentialValid)
    }
    // Selfie Tutorial Dialogs
    @Test
    fun `update selfie tutorial dialog state = false, returns false`() = runBlocking {
        signupViewModel.updateSelfieTutorialDialog(false)
        assertFalse(signupViewModel.state.getOrAwaitValueTest().showSelfieTutorial!!)
    }
    @Test
    fun `update selfie tutorial dialog state = true, returns true`() = runBlocking {
        signupViewModel.updateSelfieTutorialDialog(true)
        assertTrue(signupViewModel.state.getOrAwaitValueTest().showSelfieTutorial!!)
    }
    // Go to Credentials Entry
    @Test
    fun `Go to credentials Entry, returns true`() = runBlocking {
        signupViewModel.goToCredentialEntry()
        assertTrue(signupViewModel.state.getOrAwaitValueTest().isCredentialView)
        assertTrue(signupViewModel.state.getOrAwaitValueTest().canGoToNextScreen)
        assertNull(signupViewModel.state.getOrAwaitValueTest().firstName)
        assertNull(signupViewModel.state.getOrAwaitValueTest().lastName)
    }
    // Enables Autofocus
    @Test
    fun `enable autofocus, returns true`() = runBlocking {
        signupViewModel.enableAutofocus()
        assertTrue(signupViewModel.state.getOrAwaitValueTest().autofocusTextEnabled)
    }
    // Close privacy policy dialog
    @Test
    fun `close privacy policy dialog, returns false`() = runBlocking {
        signupViewModel.closePrivacyPolicyDialog()
        assertFalse(signupViewModel.state.getOrAwaitValueTest().showPrivacyDialog!!)
    }
    // Close Existing User Dialog
    @Test
    fun `close existing user dialog, returns true`() = runBlocking {
        signupViewModel.closeExistingUserDialog()
        assertNull(signupViewModel.state.getOrAwaitValueTest().existingUser)
    }

    @Test
    fun `WHEN form has valid names THEN isFormValid returns true`() = runTest {
        signupViewModel.updateFirstName("Caleb")
        signupViewModel.updateLastName("Kaleb")
        assertTrue(signupViewModel.isFormValid())
    }

    @Test
    fun `WHEN form has empty first name THEN isFormValid returns false`() = runTest {
        signupViewModel.updateFirstName("")
        signupViewModel.updateLastName("Kaleb")
        assertFalse(signupViewModel.isFormValid())
    }

    @Test
    fun `WHEN updateCredentialType called with Email THEN state credential is Email`() = runTest {
        signupViewModel.updateCredentialType(Credential.Email())
        assertTrue(signupViewModel.state.getOrAwaitValueTest().credential is Credential.Email)
    }

    @Test
    fun `WHEN submitInfo and user exists THEN existingUser is set`() = runTest {
        val existingUser = FakeFileGenerator.fakeUsers.first()
        signupViewModel.updatePhone("071321998893")

        coEvery { userRepo.doesUserExists("071321998893") } returns true
        coEvery { userRepo.getUserWithWallet("071321998893") } returns existingUser

        signupViewModel.submitInfo()

        assertNotNull(signupViewModel.state.getOrAwaitValueTest().existingUser)
        assertEquals(existingUser.id, signupViewModel.state.getOrAwaitValueTest().existingUser!!.id)
    }

    @Test
    fun `WHEN submitInfo and user not exists THEN navigates to name entry`() = runTest {
        signupViewModel.updatePhone("071321998893")

        coEvery { userRepo.doesUserExists("071321998893") } returns false

        signupViewModel.submitInfo()

        assertFalse(signupViewModel.state.getOrAwaitValueTest().isCredentialView)
        assertFalse(signupViewModel.state.getOrAwaitValueTest().canGoToNextScreen)
    }

    @Test
    fun `WHEN setExistingUserAsPowerUser THEN calls setPowerUserStatus`() = runTest {
        val userId = 122L
        signupViewModel.setExistingUserAsPowerUser(userId)

        coVerify { userRepo.setPowerUserStatus(userId, true) }
    }
}