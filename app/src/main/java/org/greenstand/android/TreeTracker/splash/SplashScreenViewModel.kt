package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator

class SplashScreenViewModel(
    private val preferencesMigrator: PreferencesMigrator,
    private val users: Users,
) : ViewModel() {

    fun migratePreferencesIfNeeded() {
        preferencesMigrator.migrateIfNeeded()
    }

    suspend fun isInitialSetupRequired(): Boolean = users.getPowerUser() == null
}
