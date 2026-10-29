/*
 * Copyright 2026 Treetracker
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

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [35])
class TreeSyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)
    private val syncDataUseCase = mockk<SyncDataUseCase>()
    private val syncNotificationManager = mockk<SyncNotificationManager>(relaxed = true)
    private val dao = mockk<TreeTrackerDAO>(relaxed = true)
    private val treesToSyncHelper = mockk<TreesToSyncHelper>(relaxed = true)

    private val foregroundInfo = mockk<ForegroundInfo>()
    private val foregroundUpdater = mockk<ForegroundUpdater>()
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)

    private lateinit var worker: TreeSyncWorker

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { exceptionDataCollector }
                    single { syncDataUseCase }
                    single { syncNotificationManager }
                    single { dao }
                    single { treesToSyncHelper }
                },
            )
        }

        val completedFuture = mockk<ListenableFuture<Void>>(relaxed = true)
        every { completedFuture.isDone } returns true
        every { completedFuture.isCancelled } returns false
        every { completedFuture.get() } returns null
        every { completedFuture.addListener(any(), any()) } answers { firstArg<Runnable>().run() }
        every { foregroundUpdater.setForegroundAsync(any(), any(), any()) } returns completedFuture
        every { workerParameters.foregroundUpdater } returns foregroundUpdater
        every { workerParameters.id } returns UUID.fromString("00000000-0000-0000-0000-000000000001")
        every { syncNotificationManager.createForegroundInfo(any()) } returns foregroundInfo

        worker = TreeSyncWorker(context, workerParameters)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `doWork promotes the worker to the foreground with the sync notification`() =
        runTest {
            coEvery { syncDataUseCase.execute(Unit) } returns true

            worker.doWork()

            verify { syncNotificationManager.createForegroundInfo(context) }
            verify { foregroundUpdater.setForegroundAsync(any(), any(), foregroundInfo) }
        }

    @Test
    fun `doWork returns success when sync succeeds`() =
        runTest {
            coEvery { syncDataUseCase.execute(Unit) } returns true

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }

    @Test
    fun `doWork returns failure when sync fails`() =
        runTest {
            coEvery { syncDataUseCase.execute(Unit) } returns false

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }

    @Test
    fun `doWork flags syncing for crash reports only while the sync runs`() =
        runTest {
            coEvery { syncDataUseCase.execute(Unit) } returns false

            worker.doWork()

            coVerifyOrder {
                exceptionDataCollector.set(ExceptionDataCollector.IS_SYNCING, true)
                syncDataUseCase.execute(Unit)
                exceptionDataCollector.set(ExceptionDataCollector.IS_SYNCING, false)
            }
        }

    @Test
    fun `doWork reports upload progress while the sync is running`() =
        runTest {
            val syncFinished = CompletableDeferred<Boolean>()
            every { treesToSyncHelper.getTreeCountToSync() } returns 10
            coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returns 2
            coEvery { dao.getNonUploadedTreeImageCount() } returns 1
            coEvery { syncDataUseCase.execute(Unit) } coAnswers { syncFinished.await() }
            every { syncNotificationManager.updateProgress(any(), any(), any()) } answers { syncFinished.complete(true) }

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            val expectedText = context.getString(R.string.uploading_trees) + " (7/10)"
            verify { syncNotificationManager.updateProgress(7, 10, expectedText) }
        }

    @Test
    fun `doWork never reports negative uploaded counts`() =
        runTest {
            val syncFinished = CompletableDeferred<Boolean>()
            every { treesToSyncHelper.getTreeCountToSync() } returns 2
            coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returns 4
            coEvery { dao.getNonUploadedTreeImageCount() } returns 1
            coEvery { syncDataUseCase.execute(Unit) } coAnswers { syncFinished.await() }
            every { syncNotificationManager.updateProgress(any(), any(), any()) } answers { syncFinished.complete(true) }

            worker.doWork()

            verify { syncNotificationManager.updateProgress(0, 2, context.getString(R.string.uploading_trees) + " (0/2)") }
        }

    @Test
    fun `doWork does not report progress when the sync finishes immediately`() =
        runTest {
            coEvery { syncDataUseCase.execute(Unit) } returns true

            worker.doWork()

            verify(exactly = 0) { syncNotificationManager.updateProgress(any(), any(), any()) }
        }

    @Test
    fun `getForegroundInfo creates the worker channel and an ongoing data sync notification`() =
        runTest {
            val info = worker.getForegroundInfo()

            assertEquals(NotificationConstants.WORKER_NOTIFICATION_ID, info.notificationId)
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, info.foregroundServiceType)
            val notification = info.notification
            assertEquals(NotificationConstants.WORKER_CHANNEL_ID, notification.channelId)
            assertEquals(context.getString(R.string.app_name), notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
            assertEquals(context.getString(R.string.uploading_trees), notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals(Notification.FLAG_ONGOING_EVENT, notification.flags and Notification.FLAG_ONGOING_EVENT)

            val channel =
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .getNotificationChannel(NotificationConstants.WORKER_CHANNEL_ID)
            assertEquals(NotificationConstants.WORKER_CHANNEL_NAME, channel.name.toString())
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        }
}