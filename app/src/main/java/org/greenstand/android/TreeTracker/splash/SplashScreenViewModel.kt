package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator

class SplashScreenViewModel(
    private val preferencesMigrator: PreferencesMigrator,
    private val users: Users,
) : ViewModel() {

    fun migratePreferences() {
        preferencesMigrator.migrateIfNeeded()
    }

    suspend fun requiresInitialSetup(): Boolean = users.getPowerUser() == null
}
