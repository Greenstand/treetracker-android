package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase

class SplashScreenViewModel(
    private val preferencesMigrator: PreferencesMigrator,
    private val users: Users,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val sessionTracker: SessionTracker,
    private val deviceConfigUpdater: DeviceConfigUpdater,
    private val locationDataCapturer: LocationDataCapturer,
    private val messagesRepo: MessagesRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase,
) : ViewModel() {

    suspend fun bootstrap() {
        preferencesMigrator.migrateIfNeeded()
        deviceConfigUpdater.saveLatestConfig()

        if (checkForInternetUseCase.execute(Unit)) {
            messagesRepo.syncMessages()
        }

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

    fun startGPSUpdatesForSignup() {
        locationDataCapturer.startGpsUpdates()
    }
}
