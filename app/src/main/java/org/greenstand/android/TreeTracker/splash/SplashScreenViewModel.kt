/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
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
    private val exceptionDataCollector: ExceptionDataCollector,
) : ViewModel() {

    suspend fun bootstrap() {
        deviceConfigUpdater.saveLatestConfig()

        orgRepo.init()
        orgJsonString?.let { orgRepo.addOrgFromJsonString(it) }

        if (checkForInternetUseCase.execute(Unit)) {
            messagesRepo.syncMessages()
        }

        userRepo.getPowerUser()?.let {
            exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, it.wallet)
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
        return SplashScreenViewModel(orgJsonString, get(), get(), get(), get(), get(), get(), get(), get(), get()) as T
    }
}