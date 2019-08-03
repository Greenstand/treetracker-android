package org.greenstand.android.TreeTracker.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber


class SyncBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    var onDataReceived: () -> Unit = { }

    override fun onReceive(context: Context, intent: Intent) {

        val workManager: WorkManager by inject()

        when(intent.action) {
            ACTION_STOP -> {
                workManager.cancelUniqueWork(TreeSyncWorker.UNIQUE_WORK_ID)
                NotificationManagerCompat.from(context).cancel(TreeSyncWorker.NOTIFICATION_ID)
            }
            else -> Timber.w("Unsupported action: ${intent.action}")
        }

        onDataReceived()
    }

    companion object {
        const val ACTION_STOP = "ACTION_STOP"
    }

}
