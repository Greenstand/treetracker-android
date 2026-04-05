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
package org.greenstand.android.TreeTracker.dashboard

import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkInfo.State.SUCCEEDED
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.background.NotificationConstants
import org.greenstand.android.TreeTracker.background.TreeSyncWorker
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.view.ConsumableSnackBar
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

data class DashboardState(
    val treesSynced: Int = 0,
    val treesRemainingToSync: Int = 0,
    val totalTreesToSync: Int = 0,
    val isOrgButtonEnabled: Boolean = false,
    val showUnreadMessageNotification: Boolean = false,
    val showTreeSyncReminderDialog: Boolean = false,
    val snackBar: ConsumableSnackBar? = null,
) {
    val uploadProgress: Float = if (totalTreesToSync > 0) treesRemainingToSync.toFloat() / totalTreesToSync else 0f
}

sealed class DashboardAction : Action {
    object Sync : DashboardAction()

    object SyncMessages : DashboardAction()

    object NavigateToOrg : DashboardAction()

    object NavigateToCapture : DashboardAction()

    object NavigateToMessages : DashboardAction()

    object NavigateToSettings : DashboardAction()

    object ConfirmSyncReminderDialog : DashboardAction()
}

class DashboardViewModel(
    private val dao: TreeTrackerDAO,
    private val workManager: WorkManager,
    private val analytics: Analytics,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val orgRepo: OrgRepo,
    private val messagesRepo: MessagesRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase,
    locationDataCapturer: LocationDataCapturer,
) : BaseViewModel<DashboardState, DashboardAction>(DashboardState()) {
    private var _isSyncing: Boolean? by Delegates.observable(false) { _, _, startedSyncing ->
        startedSyncing ?: return@observable
        if (startedSyncing) {
            updateTimerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(750)
                        updateData()
                    }
                }
        } else {
            updateData()
            updateTimerJob?.cancel()
            updateTimerJob = null
        }
    }

    private var updateTimerJob: Job? = null

    private fun triggerSnackBar(stringRes: Int) {
        updateState { copy(snackBar = ConsumableSnackBar(stringRes)) }
    }

    private val syncObserver =
        Observer<List<WorkInfo>> { infoList ->
            when (infoList.map { it.state }.elementAtOrNull(0)) {
                State.BLOCKED -> {
                    triggerSnackBar(R.string.sync_blocked)
                    _isSyncing = false
                }
                SUCCEEDED -> {
                    if (_isSyncing != false) {
                        triggerSnackBar(R.string.sync_successful)
                    }
                    _isSyncing = false
                }
                State.CANCELLED -> {
                    triggerSnackBar(R.string.sync_stopped)
                    _isSyncing = false
                }
                State.FAILED -> {
                    triggerSnackBar(R.string.sync_failed)
                    _isSyncing = false
                }
                State.RUNNING -> {
                    triggerSnackBar(R.string.sync_started)
                    _isSyncing = true
                }
                State.ENQUEUED -> {
                    triggerSnackBar(R.string.sync_preparing)
                    _isSyncing = true
                }

                null -> {}
            }
        }

    init {
        updateData()
        locationDataCapturer.stopGpsUpdates()
        workManager
            .getWorkInfosForUniqueWorkLiveData(NotificationConstants.UNIQUE_WORK_ID)
            .observeForever(syncObserver)
    }

    override fun handleAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.SyncMessages -> syncMessages()
            is DashboardAction.Sync -> toggleSync()
            else -> { }
        }
    }

    private fun syncMessages() {
        viewModelScope.launch {
            if (checkForInternetUseCase.execute(Unit)) {
                messagesRepo.syncMessages()
            }
        }
    }

    private fun toggleSync() {
        viewModelScope.launch {
            if (_isSyncing == false) {
                if (!FeatureFlags.DEBUG_ENABLED) {
                    val treesToSync = treesToSyncHelper.getTreeCountToSync()
                    when (treesToSync) {
                        0 -> triggerSnackBar(R.string.nothing_to_sync)
                        else -> startDataSynchronization()
                    }
                } else {
                    startDataSynchronization()
                }
                val state = currentState
                analytics.syncButtonTapped(state.totalTreesToSync, state.treesSynced, state.treesRemainingToSync)
            } else {
                updateTimerJob?.cancel()
                updateTimerJob = null
                workManager.cancelUniqueWork(NotificationConstants.UNIQUE_WORK_ID)

                val state = currentState
                analytics.stopButtonTapped(state.totalTreesToSync, state.treesSynced, state.treesRemainingToSync)
            }
        }
    }

    private fun updateData() {
        viewModelScope.launch(Dispatchers.IO) {
            val syncedTreeCount = dao.getUploadedLegacyTreeImageCount() + dao.getUploadedTreeImageCount()
            val notSyncedTreeCount = dao.getNonUploadedLegacyTreeCaptureImageCount() + dao.getNonUploadedTreeImageCount()
            val totalTreesToSync = treesToSyncHelper.getTreeCountToSync()

            val isOrgButtonEnabled = orgRepo.getOrgs().size > 1
            val hasUnreadMessages = messagesRepo.checkForUnreadMessages()

            withContext(Dispatchers.Main) {
                updateState {
                    copy(
                        totalTreesToSync = totalTreesToSync,
                        treesRemainingToSync = notSyncedTreeCount,
                        treesSynced = syncedTreeCount,
                        isOrgButtonEnabled = isOrgButtonEnabled,
                        showUnreadMessageNotification = hasUnreadMessages,
                        showTreeSyncReminderDialog = totalTreesToSync >= 2000,
                    )
                }
            }
        }
    }

    private fun startDataSynchronization() {
        val request =
            OneTimeWorkRequestBuilder<TreeSyncWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

        workManager.enqueueUniqueWork(NotificationConstants.UNIQUE_WORK_ID, ExistingWorkPolicy.KEEP, request)
    }

    override fun onCleared() {
        workManager.getWorkInfosForUniqueWorkLiveData(NotificationConstants.UNIQUE_WORK_ID).removeObserver(syncObserver)
    }
}