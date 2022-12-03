package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class SplashScreenViewModel(
    private val orgJsonString: String?,
    private val userRepo: UserRepo,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val sessionTracker: SessionTracker,
    private val deviceConfigUpdater: DeviceConfigUpdater,
    private val locationDataCapturer: LocationDataCapturer,
    private val messagesRepo: MessagesRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase,
    private val orgRepo: OrgRepo,
) : ViewModel() {

    suspend fun bootstrap() {
        deviceConfigUpdater.saveLatestConfig()

        orgRepo.init()
        orgJsonString?.let { orgRepo.addOrgFromJsonString(it) }

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

    suspend fun isInitialSetupRequired(): Boolean = userRepo.getPowerUser() == null

    fun startGPSUpdatesForSignup() {
        locationDataCapturer.startGpsUpdates()
    }
}

class SplashScreenViewModelFactory(private val orgJsonString: String?) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SplashScreenViewModel(orgJsonString, get(), get(), get(), get(), get(), get(), get(), get()) as T
    }
}
