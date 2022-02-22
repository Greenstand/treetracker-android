package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator

class SplashScreenViewModel(
    private val preferencesMigrator: PreferencesMigrator,
    private val users: Users,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val sessionTracker: SessionTracker,
) : ViewModel() {

    suspend fun bootstrap() {
        preferencesMigrator.migrateIfNeeded()

        // If session was not ended properly (user/system killed app)...
        // or we never initialized the sync count...
        // make sure the current sync count is up to date
        if (sessionTracker.wasSessionInterrupted() || treesToSyncHelper.getTreeCountToSync() == -1) {
            withContext(Dispatchers.IO) {
                treesToSyncHelper.refreshTreeCountToSync()
            }
        }
    }

    suspend fun isInitialSetupRequired(): Boolean = users.getPowerUser() == null
}
