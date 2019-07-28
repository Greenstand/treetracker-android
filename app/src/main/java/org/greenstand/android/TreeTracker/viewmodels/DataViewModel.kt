package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.usecases.SyncTreeParams
import org.greenstand.android.TreeTracker.usecases.SyncTreeUseCase
import org.greenstand.android.TreeTracker.usecases.UploadPlanterDetailsParams
import org.greenstand.android.TreeTracker.usecases.UploadPlanterDetailsUseCase
import timber.log.Timber

class DataViewModel(private val syncTreeUseCase: SyncTreeUseCase,
                    private val uploadPlanterDetailsUseCase: UploadPlanterDetailsUseCase,
                    private val api: RetrofitApi,
                    private val appDatabase: AppDatabase) : CoroutineViewModel() {

    private val treeInfoLiveData = MutableLiveData<TreeData>()
    private val toastLiveData = MutableLiveData<Int>()
    private val isSyncingLiveData = MutableLiveData<Boolean>()

    val treeData: LiveData<TreeData> = treeInfoLiveData
    val toasts: LiveData<Int> = toastLiveData
    val isSyncing: LiveData<Boolean> = isSyncingLiveData

    private var currentJob: Job? = null

    init {
        updateData()
    }

    fun sync() {
        launch {
            if (currentJob == null) {
                val treesToSync = withContext(Dispatchers.IO) { appDatabase.treeDao().getToSyncTreeCount() }
                when (treesToSync) {
                    0 -> {
                        toastLiveData.value = R.string.nothing_to_sync
                    }
                    else -> {
                        toastLiveData.value = R.string.sync_started
                        startDataSynchronization()
                    }
                }
            } else {
                currentJob?.cancel()
                currentJob = null
                isSyncingLiveData.value = false
                toastLiveData.value = R.string.sync_stopped
            }
        }
    }

    private fun updateData() {
        launch(Dispatchers.IO) {

            val treeCount = appDatabase.treeDao().getTotalTreeCount()
            val syncedTreeCount = appDatabase.treeDao().getSyncedTreeCount()
            val notSyncedTreeCount = appDatabase.treeDao().getToSyncTreeCount()

            withContext(Dispatchers.Main) {
                treeInfoLiveData.value = TreeData(totalTrees = treeCount,
                                                  treesToSync = notSyncedTreeCount,
                                                  treesSynced = syncedTreeCount)
            }
        }
    }

    private fun startDataSynchronization() {
        isSyncingLiveData.value = true
        currentJob = launch {

            val isAuthenticated = withContext(Dispatchers.IO) { api.authenticateDevice() }

            if (!isAuthenticated) {
                toastLiveData.value = R.string.sync_failed
                isSyncingLiveData.value = false
                return@launch
            }

            withContext(Dispatchers.IO) {

                uploadUserIdentifications()

                uploadNewTrees()
            }

            toastLiveData.value = R.string.sync_successful
            isSyncingLiveData.value = false
        }
    }

    private suspend fun uploadUserIdentifications() {
        // Upload all user registration data that hasn't been uploaded yet
        val registrations = appDatabase.planterDao().getPlanterRegistrationsToUpload()
        registrations.forEach {
            runCatching {
                withContext(Dispatchers.IO) { uploadPlanterDetailsUseCase.execute(UploadPlanterDetailsParams(planterDetailsId = it.id)) }
            }
        }
    }

    private suspend fun uploadNewTrees() {
        val treeList = appDatabase.treeDao().getTreesToUpload()

        Timber.tag("DataFragment").d("treeCursor: $treeList")
        Timber.tag("DataFragment").d("treeCursor: " + treeList.size)

        treeList.onEach {

            try {
                withContext(Dispatchers.IO) { syncTreeUseCase.execute(SyncTreeParams(treeId = it.tree_id)) }
            } catch (e: Exception) {
                Timber.e("NewTree upload failed")
            }

            updateData()
        }
    }

    override fun onCleared() {
        stopSyncing()
    }

    fun stopSyncing() {
        currentJob?.cancel()
        currentJob = null
        isSyncingLiveData.value = false
    }

}

data class TreeData(val treesSynced: Int,
                    val treesToSync: Int,
                    val totalTrees: Int)