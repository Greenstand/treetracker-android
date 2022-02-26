package org.greenstand.android.TreeTracker.dashboard

import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.work.*
import androidx.work.WorkInfo.*
import androidx.work.WorkInfo.State.SUCCEEDED
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.background.TreeSyncWorker
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO

data class DashboardState(
    val treesSynced: Int = 0,
    val treesRemainingToSync: Int = 0,
    val totalTreesToSync: Int = 0,
)

class DashboardViewModel(
    private val dao: TreeTrackerDAO,
    private val workManager: WorkManager,
    private val analytics: Analytics,
    private val syncNotification: SyncNotificationManager,
    private val treesToSyncHelper: TreesToSyncHelper,
) : ViewModel() {

    private val _state = MutableLiveData<DashboardState>()
    val state: LiveData<DashboardState> = _state

    var showSnackBar: ((Int) -> Unit)? = null

    private var _isSyncing: Boolean? by Delegates.observable(null) { _, _, startedSyncing ->
        startedSyncing ?: return@observable
        if (startedSyncing) {
            updateTimerJob = viewModelScope.launch {
                while(true) {
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

    private val syncObserver = Observer<List<WorkInfo>> { infoList ->
        when(infoList.map { it.state }.elementAtOrNull(0)) {
            State.BLOCKED -> {
                if (_isSyncing != null) {
                    showSnackBar?.invoke(R.string.sync_blocked)
                }

                _isSyncing = false
            }
            SUCCEEDED -> {
                if (_isSyncing != null) {
                    showSnackBar?.invoke(R.string.sync_successful)
                }

                _isSyncing = false
            }
            State.CANCELLED,
            State.FAILED -> {
                if (_isSyncing != null) {
                    showSnackBar?.invoke(R.string.sync_stopped)
                }

                _isSyncing = false
            }
            State.RUNNING -> {
                if (_isSyncing != null) {
                    showSnackBar?.invoke(R.string.sync_started)
                }

                _isSyncing = true
            }
            State.ENQUEUED -> {
                if (_isSyncing != null) {
                    showSnackBar?.invoke(R.string.sync_preparing)
                }

                _isSyncing = true
            }
        }
    }

    init {
        updateData()
        workManager.getWorkInfosForUniqueWorkLiveData(TreeSyncWorker.UNIQUE_WORK_ID)
            .observeForever(syncObserver)
    }

    fun sync() {
        viewModelScope.launch {
            if (_isSyncing == null || _isSyncing == false) {
                val treesToSync = treesToSyncHelper.getTreeCountToSync()
                when (treesToSync) {
                    0 -> showSnackBar?.invoke(R.string.nothing_to_sync)
                    else -> startDataSynchronization()
                }
                _state.value?.let { (total, synced, waiting) ->
                    analytics.syncButtonTapped(total, synced, waiting)
                }
            } else {
                updateTimerJob?.cancel()
                updateTimerJob = null
                workManager.cancelUniqueWork(TreeSyncWorker.UNIQUE_WORK_ID)

                _state.value?.let { (total, synced, waiting) ->
                    analytics.stopButtonTapped(total, synced, waiting)
                }
            }
        }
    }

    private fun updateData() {
        viewModelScope.launch(Dispatchers.IO) {
            val syncedTreeCount = dao.getUploadedTreeImageCount() + dao.getUploadedTreeCount()
            val notSyncedTreeCount = dao.getNonUploadedTreeCaptureImageCount() + dao.getNonUploadedTreeImageCount()
            val totalTreesToSync = treesToSyncHelper.getTreeCountToSync()

            withContext(Dispatchers.Main) {
                _state.value = DashboardState(
                    totalTreesToSync = totalTreesToSync,
                    treesRemainingToSync = notSyncedTreeCount,
                    treesSynced = syncedTreeCount,
                )
            }
        }
    }

    private fun startDataSynchronization() {
        val request = OneTimeWorkRequestBuilder<TreeSyncWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(TreeSyncWorker.UNIQUE_WORK_ID, ExistingWorkPolicy.KEEP, request)
    }

    override fun onCleared() {
        workManager.getWorkInfosForUniqueWorkLiveData(TreeSyncWorker.UNIQUE_WORK_ID).removeObserver(syncObserver)
    }

}