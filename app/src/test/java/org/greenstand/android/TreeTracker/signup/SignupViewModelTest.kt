package org.greenstand.android.TreeTracker.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utilities.Validation
import org.greenstand.android.TreeTracker.utils.fakeSignUpState
import org.greenstand.android.TreeTracker.utils.fakeUsers
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sign

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

    @Test
    fun `update email contains valid credentials, returns success`() = runBlocking{
        signupViewModel.updateEmail("somerandom@gmsail.com")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().email).containsMatch("@")
    }

    @Test
    fun `update email contains invalid credentials, returns Error`() = runBlocking{
        signupViewModel.updateEmail("somerandom!!gmsail.com")
        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().email).doesNotContainMatch("@")
    }

//    @Test
//    fun `update phone contains valid credentials, returns success`() = runBlocking{
//        val result = signupViewModel.updatePhone("01111111111")
//        val validation  = Validation.isValidPhoneNumber("01111111")
//        assert
//    }

//    @Test
//    fun `update credential type,returns success`() = runBlocking{
//        signupViewModel.updateCredentialType()
//        Truth.assertThat(signupViewModel.state.getOrAwaitValueTest().email).contains(fakeSignUpState.credential.text)
//    }
}








































