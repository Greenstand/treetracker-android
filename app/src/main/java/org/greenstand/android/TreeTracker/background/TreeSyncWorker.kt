package org.greenstand.android.TreeTracker.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named

class TreeSyncWorker(context: Context,
                     workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters), KoinComponent {

    override suspend fun doWork(): Result {
        //TODO: Would like to move those 2 injections to constructor, but I am not sure if it will still be lazy there.
        val syncDataUseCase: SyncDataUseCase by inject(named(SyncDataUseCase.CONTINUOUS_UPLOAD))
        val syncDataBundleUseCase: SyncDataUseCase by inject(named(SyncDataUseCase.BUNDLE_UPLOAD))
        val syncNotificationManager: SyncNotificationManager by inject()

        syncNotificationManager.showNotification()

        if (FeatureFlags.BULK_UPLOAD_ENABLED) {
            syncDataBundleUseCase.execute(Unit)
        } else {
            syncDataUseCase.execute(Unit)
        }

        syncNotificationManager.removeNotification()

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_ID = "UNIQUE_WORK_ID_TREE_SYNC_WORKER"
    }
}