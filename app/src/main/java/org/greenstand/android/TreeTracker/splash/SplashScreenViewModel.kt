package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator

class SplashScreenViewModel(
    private val preferencesMigrator: PreferencesMigrator
) : ViewModel() {

    fun migratePreferences() {
        preferencesMigrator.migrateIfNeeded()
    }
}
