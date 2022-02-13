package org.greenstand.android.TreeTracker.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.koin.core.KoinComponent
import org.koin.core.inject

class TreeSyncWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KoinComponent {
    private val syncDataBundleUseCase: SyncDataUseCase by inject()
    private val syncNotificationManager: SyncNotificationManager by inject()

    override suspend fun doWork(): Result {
        setForeground(syncNotificationManager.createForegroundInfo(applicationContext, id))
        syncDataBundleUseCase.execute(Unit)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_ID = "UNIQUE_WORK_ID_TREE_SYNC_WORKER"
    }
}
