package org.greenstand.android.TreeTracker.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.fakeUsers
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
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
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().firstName).contains(
            fakeUsers.first().firstName
        )
    }

    @Test
    fun `Blank first name, returns Empty input `() = runBlocking{
        signupViewModel.updateFirstName("")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().firstName).isEmpty()
    }

    // Last Name

    @Test
    fun `update last name, returns valid last name`() = runBlocking{
        signupViewModel.updateLastName("Kaleb")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().lastName).contains(
            fakeUsers.first().lastName
        )
    }

    @Test
    fun `Blank last name, returns Empty input `() = runBlocking{
        signupViewModel.updateLastName("")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().lastName).isEmpty()
    }

    // Email

    @Test
    fun `update email contains valid credentials, returns success`() = runBlocking{
        signupViewModel.updateEmail("somerandom@gmsail.com")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().phone).isNull()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().email).containsMatch("@")


    }

    @Test
    fun `update email contains invalid credentials, returns Error`() = runBlocking{
        signupViewModel.updateEmail("somerandom!!gmsail.com")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().email).doesNotContainMatch("@")
    }

    // Phone

    @Test
    fun `update phone contains valid credentials, returns success`() = runBlocking{
        signupViewModel.updatePhone("071321998893")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().isCredentialValid).isTrue()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().email).isNull()

    }

    @Test
    fun `update phone contains invalid credentials, returns Error`() = runBlocking{
        signupViewModel.updatePhone("random1919")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().isCredentialValid).isFalse()
    }

    // Selfie Tutorial Dialogs
    @Test
    fun `update selfie tutorial dialog state = false, returns false`() = runBlocking{
        signupViewModel.updateSelfieTutorialDialog(false)
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().showSelfieTutorial).isFalse()
    }

    @Test
    fun `update selfie tutorial dialog state = true, returns true`() = runBlocking{
        signupViewModel.updateSelfieTutorialDialog(true)
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().showSelfieTutorial).isTrue()
    }

    // Go to Credentials Entry
    @Test
    fun `Go to credentials Entry, returns true`() = runBlocking{
        signupViewModel.goToCredentialEntry()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().isCredentialView).isTrue()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().firstName).isNull()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().lastName).isNull()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().canGoToNextScreen).isTrue()
    }

    // Enables Autofocus
    @Test
    fun `enable autofocus, returns true`() = runBlocking{
        signupViewModel.enableAutofocus()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().autofocusTextEnabled).isTrue()
    }

    // Close privacy policy dialog
    @Test
    fun `close privacy policy dialog, returns true`() = runBlocking{
        signupViewModel.closePrivacyPolicyDialog()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().showPrivacyDialog).isFalse()
    }

    // Close Existing User Dialog
    @Test
    fun `close existing user dialog, returns true`() = runBlocking{
        signupViewModel.closeExistingUserDialog()
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().existingUser).isNull()
    }

}








































