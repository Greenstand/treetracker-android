package org.greenstand.android.TreeTracker.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert.*
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
    private lateinit var signupViewModel:SignupViewModel

    @Before
    fun setupViewModel(){
        signupViewModel = SignupViewModel(userRepo, checkForInternetUseCase)
    }
    // First Name
    @Test
    fun `update first name, returns valid first name`() = runBlocking{
        signupViewModel.updateFirstName("Caleb")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().firstName, FakeFileGenerator.fakeUsers.first().firstName)
    }
    @Test
    fun `Blank first name, returns Empty input `() = runBlocking{
        signupViewModel.updateFirstName("")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().firstName, FakeFileGenerator.emptyUser.firstName)
    }
    // Last Name
    @Test
    fun `update last name, returns valid last name`() = runBlocking{
        signupViewModel.updateLastName("Kaleb")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().lastName, FakeFileGenerator.fakeUsers.first().lastName)
    }
    @Test
    fun `Blank last name, returns Empty input `() = runBlocking{
        signupViewModel.updateLastName("")
        assertEquals(signupViewModel.state.getOrAwaitValueTest().lastName, FakeFileGenerator.emptyUser.lastName)
    }
    // Email
    @Test
    fun `update email contains valid credentials, returns success`() = runBlocking{
        signupViewModel.updateEmail("somerandom@gmsail.com")
        assertNull(signupViewModel.state.getOrAwaitValueTest().phone)
        assertEquals(signupViewModel.state.getOrAwaitValueTest().email, "somerandom@gmsail.com")

    }
    @Test
    fun `update email contains invalid credentials, returns Error`() = runBlocking{
        signupViewModel.updateEmail("somerandom!!gmsail.com")
        assertNotEquals(signupViewModel.state.getOrAwaitValueTest().email, "somerandom@gmsail.com")
    }
    // Phone
    @Test
    fun `update phone contains valid credentials, returns success`() = runBlocking{
        signupViewModel.updatePhone("071321998893")
        assertTrue(signupViewModel.state.getOrAwaitValueTest().isCredentialValid)
        assertNull(signupViewModel.state.getOrAwaitValueTest().email)

    }
    @Test
    fun `update phone contains invalid credentials, returns Error`() = runBlocking{
        signupViewModel.updatePhone("random1919")
        assertFalse(signupViewModel.state.getOrAwaitValueTest().isCredentialValid)
    }
    // Selfie Tutorial Dialogs
    @Test
    fun `update selfie tutorial dialog state = false, returns false`() = runBlocking{
        signupViewModel.updateSelfieTutorialDialog(false)
        assertFalse(signupViewModel.state.getOrAwaitValueTest().showSelfieTutorial!!)
    }
    @Test
    fun `update selfie tutorial dialog state = true, returns true`() = runBlocking{
        signupViewModel.updateSelfieTutorialDialog(true)
        assertTrue(signupViewModel.state.getOrAwaitValueTest().showSelfieTutorial!!)
    }
    // Go to Credentials Entry
    @Test
    fun `Go to credentials Entry, returns true`() = runBlocking{
        signupViewModel.goToCredentialEntry()
        assertTrue(signupViewModel.state.getOrAwaitValueTest().isCredentialView)
        assertTrue(signupViewModel.state.getOrAwaitValueTest().canGoToNextScreen)
        assertNull(signupViewModel.state.getOrAwaitValueTest().firstName)
        assertNull(signupViewModel.state.getOrAwaitValueTest().lastName)
    }
    // Enables Autofocus
    @Test
    fun `enable autofocus, returns true`() = runBlocking{
        signupViewModel.enableAutofocus()
        assertTrue(signupViewModel.state.getOrAwaitValueTest().autofocusTextEnabled)
    }
    // Close privacy policy dialog
    @Test
    fun `close privacy policy dialog, returns false`() = runBlocking{
        signupViewModel.closePrivacyPolicyDialog()
        assertFalse(signupViewModel.state.getOrAwaitValueTest().showPrivacyDialog!!)
    }
    // Close Existing User Dialog
    @Test
    fun `close existing user dialog, returns true`() = runBlocking{
        signupViewModel.closeExistingUserDialog()
        assertNull(signupViewModel.state.getOrAwaitValueTest().existingUser)
    }
}