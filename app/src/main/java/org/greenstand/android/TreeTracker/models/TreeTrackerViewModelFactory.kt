package org.greenstand.android.TreeTracker.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.RuntimeException
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.splash.SplashScreenViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.koin.core.KoinComponent

@Suppress("UNCHECKED_CAST")
class TreeTrackerViewModelFactory(
    private val splashScreenViewModel: SplashScreenViewModel,
    private val languagePickerViewModel: LanguagePickerViewModel,
    private val dashboardViewModel: DashboardViewModel,
    private val signupViewModel: SignupViewModel,
    private val userSelectViewModel: UserSelectViewModel,
    private val orgPickerViewModel: OrgPickerViewModel,
) : ViewModelProvider.NewInstanceFactory(), KoinComponent {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UserSelectViewModel::class.java) -> userSelectViewModel as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> dashboardViewModel as T
            modelClass.isAssignableFrom(SignupViewModel::class.java) -> signupViewModel as T
            modelClass.isAssignableFrom(LanguagePickerViewModel::class.java) -> languagePickerViewModel as T
            modelClass.isAssignableFrom(OrgPickerViewModel::class.java) -> orgPickerViewModel as T
            modelClass.isAssignableFrom(SplashScreenViewModel::class.java) -> splashScreenViewModel as T
            else -> throw RuntimeException("Unable to create instance of ${modelClass.simpleName}. Did you forget to update the TreeTrackerViewModelFactory?")
        }
    }
}
