/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import java.util.UUID
import org.greenstand.android.TreeTracker.R

class SyncNotificationManager(
    private val notificationManagerCompat: NotificationManagerCompat,
) {

    fun createForegroundInfo(applicationContext: Context, workerId: UUID): ForegroundInfo {
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(workerId)

        val builder = NotificationCompat.Builder(applicationContext, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.greenstand_logo)
            .setContentTitle(applicationContext.getString(R.string.syncing))
            .setContentText(applicationContext.getString(R.string.uploading_trees))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(applicationContext.resources.getColor(R.color.colorPrimary))
            .addAction(android.R.drawable.ic_delete, applicationContext.getString(R.string.cancel), intent)
            .setProgress(0, 0, true)

        return ForegroundInfo(NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(SYNC_CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
        }
        notificationManagerCompat.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 212
        private const val SYNC_CHANNEL_ID = "org.greenstand.android.TreeTracker.background.SyncNotificationManager"
        private const val CHANNEL_NAME = "Sync Channel"
        private const val CHANNEL_DESCRIPTION = "Tree Syncing"
    }
}