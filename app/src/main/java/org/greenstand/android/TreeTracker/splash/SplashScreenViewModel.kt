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
import org.greenstand.android.TreeTracker.models.organization.OrgConfigProvider
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

data class SplashState(
    val isBootstrapping: Boolean = true,
)

sealed class SplashAction : Action {
    object StartGPSUpdatesForSignup : SplashAction()
}

class SplashScreenViewModel(
    private val orgId: String?,
    private val orgName: String?,
    private val userRepo: UserRepo,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val sessionTracker: SessionTracker,
    private val deviceConfigUpdater: DeviceConfigUpdater,
    private val locationDataCapturer: LocationDataCapturer,
    private val messagesRepo: MessagesRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase,
    private val orgRepo: OrgRepo,
    private val orgConfigProvider: OrgConfigProvider,
    private val exceptionDataCollector: ExceptionDataCollector,
) : BaseViewModel<SplashState, SplashAction>(SplashState()) {
    override fun handleAction(action: SplashAction) {
        when (action) {
            is SplashAction.StartGPSUpdatesForSignup -> {
                locationDataCapturer.startGpsUpdates()
            }
        }
    }

    suspend fun bootstrap() {
        deviceConfigUpdater.saveLatestConfig()

        orgRepo.init()
        if (!orgId.isNullOrBlank()) {
            Timber.tag(ORG_LINK_TAG).i("Deeplink received: orgId=$orgId, orgName=$orgName")
            val configJson = orgConfigProvider.fetchOrgConfig(orgId)
            if (configJson != null) {
                Timber.tag(ORG_LINK_TAG).i("Remote Config found for org $orgId, applying full config")
                orgRepo.addOrgFromRemoteConfig(orgId, orgName ?: "", configJson)
            } else {
                Timber.tag(ORG_LINK_TAG).i("No Remote Config for org $orgId, creating minimal org with defaults")
                orgRepo.addMinimalOrg(orgId, orgName ?: "")
            }
        } else {
            Timber.tag(ORG_LINK_TAG).d("No org deeplink, using existing org")
            // Non-deeplink launch: refresh current org config if we haven't yet
            val currentOrg = orgRepo.currentOrg()
            if (currentOrg.id != OrgRepo.DEFAULT_ORG_ID && !orgRepo.hasCompletedInitialOrgSync()) {
                val configJson = orgConfigProvider.fetchOrgConfig(currentOrg.id)
                Timber.tag(ORG_LINK_TAG).d("Fetching Remote Config for org ${currentOrg.id}")
                if (configJson != null) {
                    orgRepo.addOrgFromRemoteConfig(currentOrg.id, currentOrg.name, configJson)
                    Timber.tag(ORG_LINK_TAG).i("Add Org From Remote Config ${currentOrg.id}")
                    orgRepo.markInitialOrgSyncComplete()
                    Timber.tag(ORG_LINK_TAG).i("Mark Initial Org Sync Complete ${currentOrg.id}")
                }
            }
        }

        if (checkForInternetUseCase.execute(Unit)) {
            messagesRepo.syncMessages()
        }

        userRepo.getPowerUser()?.let {
            exceptionDataCollector.set(ExceptionDataCollector.POWER_USER_WALLET, it.wallet)
        }

        if (sessionTracker.wasSessionInterrupted() || treesToSyncHelper.getTreeCountToSync() == -1) {
            withContext(Dispatchers.IO) {
                treesToSyncHelper.refreshTreeCountToSync()
            }
        }
    }

    suspend fun isInitialSetupRequired(): Boolean = userRepo.getPowerUser() == null

    companion object {
        private const val ORG_LINK_TAG = "OrgLink"
    }
}

class SplashScreenViewModelFactory(
    private val orgId: String?,
    private val orgName: String?,
) : ViewModelProvider.Factory,
    KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SplashScreenViewModel(orgId, orgName, get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) as T
}