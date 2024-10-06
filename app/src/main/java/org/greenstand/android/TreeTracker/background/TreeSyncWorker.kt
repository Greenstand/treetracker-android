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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.TreeTrackerActivity
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TreeSyncWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KoinComponent {

    private val exceptionDataCollector: ExceptionDataCollector by inject()
    private val syncDataBundleUseCase: SyncDataUseCase by inject()
    private val syncNotificationManager: SyncNotificationManager by inject()

    override suspend fun doWork(): Result {
        setForeground(syncNotificationManager.createForegroundInfo(applicationContext, id))
        exceptionDataCollector.set(ExceptionDataCollector.IS_SYNCING, true)
        val result = syncDataBundleUseCase.execute(Unit)
        exceptionDataCollector.set(ExceptionDataCollector.IS_SYNCING, false)
        return if (result) Result.success() else Result.failure()
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

        val pendingFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, TreeTrackerActivity::class.java), pendingFlag))
            .setSmallIcon(R.drawable.upload_icon)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentText(applicationContext.getString(R.string.uploading_trees))
            .build()
        return ForegroundInfo(1337, notification)
    }
}