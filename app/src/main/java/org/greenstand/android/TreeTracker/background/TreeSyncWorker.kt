package org.greenstand.android.TreeTracker.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
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
        private const val NOTIFICATION_CHANNEL_ID = "11"
        private const val NOTIFICATION_CHANNEL_NAME = "Work Service"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val PENDING_INTENT_FLAG_MUTABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PENDING_INTENT_FLAG_MUTABLE))
            .setSmallIcon(R.drawable.upload_icon)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentText("Updating widget")
            .build()
        return ForegroundInfo(1337, notification)
    }
}
