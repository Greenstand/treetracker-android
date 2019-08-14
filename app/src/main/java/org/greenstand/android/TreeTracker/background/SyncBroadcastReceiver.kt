package org.greenstand.android.TreeTracker.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber


class SyncBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {

        val workManager: WorkManager by inject()
        val syncNotification: SyncNotificationManager by inject()

        when(intent.action) {
            ACTION_STOP -> {
                workManager.cancelUniqueWork(TreeSyncWorker.UNIQUE_WORK_ID)
                syncNotification.removeNotification()
            }
            else -> Timber.w("Unsupported action: ${intent.action}")
        }
    }

    companion object {
        const val ACTION_STOP = "ACTION_STOP"
    }

}
