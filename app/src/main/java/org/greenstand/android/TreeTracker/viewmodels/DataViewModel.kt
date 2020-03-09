package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.*
import androidx.work.*
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.background.TreeSyncWorker
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.PlanterAccountEntity
import org.greenstand.android.TreeTracker.managers.UserManager
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class DataViewModel(private val userManager: UserManager,
                    private val dao: TreeTrackerDAO,
                    private val workManager: WorkManager,
                    private val analytics: Analytics,
                    private val syncNotification: SyncNotificationManager) : ViewModel() {

    private val treeInfoLiveData = MutableLiveData<PlanterAccountData>()
    private val toastLiveData = MutableLiveData<Int>()
    private val isSyncingLiveData = MutableLiveData<Boolean>()

    val planterAccountData: LiveData<PlanterAccountData> = treeInfoLiveData
    val toasts: LiveData<Int> = toastLiveData
    val isSyncing: LiveData<Boolean> = isSyncingLiveData

    private var _isSyncing: Boolean? by Delegates.observable<Boolean?>(null) { _, _, startedSyncing ->

        startedSyncing ?: return@observable

        if (startedSyncing) {
            updateTimerJob = viewModelScope.launch {
                while(true) {
                    delay(750)
                    updateData()
                }
            }
        }
    }

    private var updateTimerJob: Job? = null

    private val syncObserver = Observer<List<WorkInfo>> { infoList ->
        when(infoList.map { it.state }.elementAtOrNull(0)) {
            WorkInfo.State.BLOCKED -> {
                if (_isSyncing != null) {
                    toastLiveData.value = R.string.sync_blocked
                }

                _isSyncing = false
                isSyncingLiveData.value = false
            }
            WorkInfo.State.SUCCEEDED,
            WorkInfo.State.CANCELLED,
            WorkInfo.State.FAILED -> {
                if (_isSyncing != null) {
                    toastLiveData.value = R.string.sync_stopped
                }

                _isSyncing = false
                isSyncingLiveData.value = false
            }
            WorkInfo.State.RUNNING -> {
                if (_isSyncing != null) {
                    toastLiveData.value = R.string.sync_started
                }

                _isSyncing = true
                isSyncingLiveData.value = true
            }
            else -> { }
        }
    }

    init {
        updateData()

        workManager.getWorkInfosForUniqueWorkLiveData(TreeSyncWorker.UNIQUE_WORK_ID)
            .observeForever(syncObserver)

    }

    fun sync() {
        viewModelScope.launch {
            val treeData = loadPlanterAccountData().treeData
            val totalCount = treeData.uploadedCount + treeData.waitingToUploadCount
            if (_isSyncing == null || _isSyncing == false) {
                val treesToSync =
                    withContext(Dispatchers.IO) { dao.getNonUploadedTreeCaptureCount() }
                if (treesToSync == 0) {
                    toastLiveData.value = R.string.nothing_to_sync
                    isSyncingLiveData.value = false
                } else {
                    isSyncingLiveData.value = true
                    startDataSynchronization()
                }
                analytics.syncButtonTapped(totalCount, treeData.uploadedCount, treeData.waitingToUploadCount)
            } else {
                updateTimerJob?.cancel()
                updateTimerJob = null
                workManager.cancelUniqueWork(TreeSyncWorker.UNIQUE_WORK_ID)
                syncNotification.removeNotification()
                isSyncingLiveData.value = false
                analytics.stopButtonTapped(totalCount, treeData.uploadedCount, treeData.waitingToUploadCount)
            }
        }
    }

    private suspend fun loadPlanterAccountData(): PlanterAccountData {
        return withContext(Dispatchers.IO) {
            val planterAccountEntity: PlanterAccountEntity? = dao.getPlanterAccount(userManager.planterInfoId)
            val uploadedTreeCount = planterAccountEntity?.uploadedTreeCount ?: 0
            val validatedTreeCount = planterAccountEntity?.validatedTreeCount ?: 0
            val totalAmountPaid = planterAccountEntity?.totalAmountPaid ?: 0.0
            val paymentAmountPending = planterAccountEntity?.paymentAmountPending ?: 0.0
            val waitingToUploadCount = dao.getNonUploadedTreeImageCount()
            val treeData = TreeData(uploadedCount = uploadedTreeCount,
                     waitingToUploadCount = waitingToUploadCount,
                     validatedCount = validatedTreeCount)
            PlanterAccountData(
                treeData = treeData,
                totalAmountPaid = totalAmountPaid,
                paymentAmountPending = paymentAmountPending
            )
        }
    }

    private fun updateData() {
        viewModelScope.launch(Dispatchers.IO) {

            val treeInfo = loadPlanterAccountData()

            withContext(Dispatchers.Main) {
                treeInfoLiveData.value = treeInfo
            }
        }
    }

    private fun startDataSynchronization() {
        val request = OneTimeWorkRequestBuilder<TreeSyncWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(TreeSyncWorker.UNIQUE_WORK_ID, ExistingWorkPolicy.KEEP, request)
    }

    override fun onCleared() {
        workManager.getWorkInfosForUniqueWorkLiveData(TreeSyncWorker.UNIQUE_WORK_ID).removeObserver(syncObserver)
    }
}

data class TreeData(
    val uploadedCount: Int,
    val waitingToUploadCount: Int,
    val validatedCount: Int
)

data class PlanterAccountData(
    val treeData: TreeData,
    val totalAmountPaid: Double,
    val paymentAmountPending: Double
)
