package org.greenstand.android.TreeTracker.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.greenstand.android.TreeTracker.R

class SyncNotificationManager(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val context: Context
) {

    fun showNotification() {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.greenstand_logo)
            .setContentTitle(context.getString(R.string.syncing))
            .setContentText(context.getString(R.string.uploading_trees))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(context.resources.getColor(R.color.colorPrimary))
            .setProgress(0, 0, true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun removeNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createStopSyncPendingIntent(): PendingIntent {
        val intent = Intent(context, SyncBroadcastReceiver::class.java).apply {
            action = SyncBroadcastReceiver.ACTION_STOP
            putExtra(Notification.EXTRA_NOTIFICATION_ID, 0)
        }

        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(SYNC_CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManagerCompat.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 212
        private const val SYNC_CHANNEL_ID = "org.greenstand.android.TreeTracker.background.SyncNotificationManager"
        private const val CHANNEL_NAME = "Sync Channel"
        private const val CHANNEL_DESCRIPTION = "Tree Syncing"
    }
}
