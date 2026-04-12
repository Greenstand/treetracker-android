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

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import org.greenstand.android.TreeTracker.R

class SyncNotificationManager(
    private val notificationManagerCompat: NotificationManagerCompat,
) {
    private var applicationContext: Context? = null

    fun createForegroundInfo(
        applicationContext: Context,
    ): ForegroundInfo {
        this.applicationContext = applicationContext

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val builder =
            NotificationCompat
                .Builder(applicationContext, NotificationConstants.SYNC_CHANNEL_ID)
                .setSmallIcon(R.drawable.greenstand_logo)
                .setContentTitle(applicationContext.getString(R.string.syncing))
                .setContentText(applicationContext.getString(R.string.uploading_trees))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(
                    androidx.core.content.ContextCompat
                        .getColor(applicationContext, R.color.colorPrimary),
                ).setProgress(0, 0, true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationConstants.SYNC_NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationConstants.SYNC_NOTIFICATION_ID, builder.build())
        }
    }

    // Foreground service notifications are exempt from POST_NOTIFICATIONS permission,
    // so the MissingPermission lint warning is a false positive here.
    @SuppressLint("MissingPermission")
    fun updateProgress(
        progress: Int,
        max: Int,
        contentText: String,
    ) {
        val context = applicationContext ?: return
        val builder =
            NotificationCompat
                .Builder(context, NotificationConstants.SYNC_CHANNEL_ID)
                .setSmallIcon(R.drawable.greenstand_logo)
                .setContentTitle(context.getString(R.string.syncing))
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setOnlyAlertOnce(true)
                .setProgress(max, progress, false)
        notificationManagerCompat.notify(NotificationConstants.SYNC_NOTIFICATION_ID, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel =
            NotificationChannel(NotificationConstants.SYNC_CHANNEL_ID, NotificationConstants.SYNC_CHANNEL_NAME, importance).apply {
                description = NotificationConstants.SYNC_CHANNEL_DESCRIPTION
            }
        notificationManagerCompat.createNotificationChannel(channel)
    }
}