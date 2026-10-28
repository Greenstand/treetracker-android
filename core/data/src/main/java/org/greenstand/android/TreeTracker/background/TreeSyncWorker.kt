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
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.resources.R
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TreeSyncWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters),
    KoinComponent {
    private val exceptionDataCollector: ExceptionDataCollector by inject()
    private val syncDataBundleUseCase: SyncDataUseCase by inject()
    private val syncNotificationManager: SyncNotificationManager by inject()
    private val dao: TreeTrackerDAO by inject()
    private val treesToSyncHelper: TreesToSyncHelper by inject()

    override suspend fun doWork(): Result {
        setForeground(syncNotificationManager.createForegroundInfo(applicationContext))

        val totalTreesToSync = treesToSyncHelper.getTreeCountToSync()

        return coroutineScope {
            val progressJob =
                launch {
                    while (true) {
                        delay(750)
                        val remaining =
                            withContext(Dispatchers.IO) {
                                dao.getNonUploadedLegacyTreeCaptureImageCount() + dao.getNonUploadedTreeImageCount()
                            }
                        val uploaded = (totalTreesToSync - remaining).coerceAtLeast(0)
                        val contentText = applicationContext.getString(R.string.uploading_trees) + " ($uploaded/$totalTreesToSync)"
                        syncNotificationManager.updateProgress(uploaded, totalTreesToSync, contentText)
                    }
                }

            exceptionDataCollector.set(ExceptionDataCollector.IS_SYNCING, true)
            val result = syncDataBundleUseCase.execute(Unit)
            exceptionDataCollector.set(ExceptionDataCollector.IS_SYNCING, false)
            progressJob.cancel()
            if (result) Result.success() else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NotificationConstants.WORKER_CHANNEL_ID,
                    NotificationConstants.WORKER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val notification =
            NotificationCompat
                .Builder(applicationContext, NotificationConstants.WORKER_CHANNEL_ID)
                .setContentIntent(PendingIntent.getActivity(applicationContext, 0, mainActivityIntent(), pendingFlag))
                .setSmallIcon(R.drawable.upload_icon)
                .setOngoing(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentTitle(appName())
                .setLocalOnly(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentText(applicationContext.getString(R.string.uploading_trees))
                .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationConstants.WORKER_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationConstants.WORKER_NOTIFICATION_ID, notification)
        }
    }

    // The worker lives in core:data, which can't see the app's activity class or the app_name
    // string (a per-build-type resValue in :app). Both are resolved at runtime instead, with the
    // same result: an explicit intent for TreeTrackerActivity, and the app's label (@string/app_name).
    private fun mainActivityIntent(): Intent = Intent().setClassName(applicationContext, NotificationConstants.MAIN_ACTIVITY_CLASS)

    private fun appName(): String =
        applicationContext.applicationInfo
            .loadLabel(applicationContext.packageManager)
            .toString()
}