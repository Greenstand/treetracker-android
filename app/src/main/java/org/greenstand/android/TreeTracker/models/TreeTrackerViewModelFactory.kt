package org.greenstand.android.TreeTracker.models

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.RuntimeException
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.splash.SplashScreenViewModel

@Suppress("UNCHECKED_CAST")
class TreeTrackerViewModelFactory(
    private val preferencesMigrator: PreferencesMigrator,
    private val languageSwitcher: LanguageSwitcher,
    private val resources: Resources
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashScreenViewModel::class.java) -> SplashScreenViewModel(preferencesMigrator) as T
            modelClass.isAssignableFrom(LanguagePickerViewModel::class.java) -> LanguagePickerViewModel(languageSwitcher, resources) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel() as T
            modelClass.isAssignableFrom(SignupViewModel::class.java) -> SignupViewModel() as T
            else -> throw RuntimeException("Unable to create instance of ${modelClass.simpleName}. Did you forget to update the TreeTrackerViewModelFactory?")
        }
    }
}
