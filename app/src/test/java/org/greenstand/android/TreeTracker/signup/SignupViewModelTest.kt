package org.greenstand.android.TreeTracker.signup

import com.google.common.truth.Truth
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class SignupViewModelTest {

    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>(relaxed = true)
    private val signupViewModel = SignupViewModel(userRepo, checkForInternetUseCase)

    @Test
    fun `update first name, returns valid first name`() = runTest {
        signupViewModel.updateFirstName("caleb")
        Truth.assertThat(signupViewModel.state.value?.firstName).isNotNull()
    }

}







































