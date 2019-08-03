package org.greenstand.android.TreeTracker.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.greenstand.android.TreeTracker.viewmodels.SyncDataUseCase
import org.koin.core.KoinComponent
import org.koin.core.inject

class TreeSyncWorker(context: Context,
                     workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters), KoinComponent {

    override suspend fun doWork(): Result {

        val syncDataUseCase: SyncDataUseCase by inject()

        syncDataUseCase.execute(Unit)

        return Result.success()
    }


    companion object {
        const val UNIQUE_WORK_ID = "UNIQUE_WORK_ID_TREE_SYNC_WORKER"
    }

}