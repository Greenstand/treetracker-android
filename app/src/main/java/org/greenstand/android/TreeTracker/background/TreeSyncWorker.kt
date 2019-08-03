package org.greenstand.android.TreeTracker.background

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.viewmodels.SyncDataUseCase
import org.koin.core.KoinComponent
import org.koin.core.inject

class TreeSyncWorker(context: Context,
                     workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters), KoinComponent {

    override suspend fun doWork(): Result {

        val syncDataUseCase: SyncDataUseCase by inject()

        createNotificationChannel()

        var builder = NotificationCompat.Builder(applicationContext, "channel_id")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Syncing...")
            .setContentText("Uploading all trees")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.save_icon, "Stop", createStopSyncPendingIntent())

        val notification = builder.build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)

        syncDataUseCase.execute(Unit)

        return Result.success()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = applicationContext.getString(R.string.channel_name)
//            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id", "channel name", importance).apply {
                description = "Description"
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createStopSyncPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, SyncBroadcastReceiver::class.java).apply {
            action = SyncBroadcastReceiver.ACTION_STOP
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }

        return PendingIntent.getBroadcast(applicationContext, 0, intent, 0)
    }

    companion object {
        const val UNIQUE_WORK_ID = "UNIQUE_WORK_ID_TREE_SYNC_WORKER"
        const val NOTIFICATION_ID = 212
    }


}