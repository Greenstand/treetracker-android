package org.greenstand.android.TreeTracker.splash

import org.greenstand.android.TreeTracker.BaseViewModel
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator

class SplashScreenViewModel(
    private val preferencesMigrator: PreferencesMigrator
) : BaseViewModel() {

    fun migratePreferences() {
        preferencesMigrator.migrateIfNeeded()
    }
}
