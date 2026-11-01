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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.greenstand.android.TreeTracker.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [35])
class SyncNotificationManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private lateinit var syncNotificationManager: SyncNotificationManager

    @Before
    fun setUp() {
        notificationManagerCompat = mockk(relaxed = true)
        syncNotificationManager = SyncNotificationManager(notificationManagerCompat)
    }

    @Test
    fun `createForegroundInfo creates the sync notification channel`() {
        val channelSlot = slot<NotificationChannel>()
        every { notificationManagerCompat.createNotificationChannel(capture(channelSlot)) } returns Unit

        syncNotificationManager.createForegroundInfo(context)

        val channel = channelSlot.captured
        assertEquals(NotificationConstants.SYNC_CHANNEL_ID, channel.id)
        assertEquals(NotificationConstants.SYNC_CHANNEL_NAME, channel.name.toString())
        assertEquals(NotificationConstants.SYNC_CHANNEL_DESCRIPTION, channel.description)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun `createForegroundInfo returns a data sync foreground notification with indeterminate progress`() {
        val foregroundInfo = syncNotificationManager.createForegroundInfo(context)

        assertEquals(NotificationConstants.SYNC_NOTIFICATION_ID, foregroundInfo.notificationId)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, foregroundInfo.foregroundServiceType)
        val notification = foregroundInfo.notification
        assertEquals(NotificationConstants.SYNC_CHANNEL_ID, notification.channelId)
        assertEquals(context.getString(R.string.syncing), notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(context.getString(R.string.uploading_trees), notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
    }

    @Test
    fun `updateProgress before the foreground info is created does nothing`() {
        syncNotificationManager.updateProgress(progress = 1, max = 10, contentText = "Uploading trees (1/10)")

        verify(exactly = 0) { notificationManagerCompat.notify(any<Int>(), any<Notification>()) }
    }

    @Test
    fun `updateProgress posts the sync notification with determinate progress`() {
        val notificationSlot = slot<Notification>()
        every { notificationManagerCompat.notify(any<Int>(), capture(notificationSlot)) } returns Unit
        syncNotificationManager.createForegroundInfo(context)

        syncNotificationManager.updateProgress(progress = 3, max = 10, contentText = "Uploading trees (3/10)")

        verify(exactly = 1) { notificationManagerCompat.notify(NotificationConstants.SYNC_NOTIFICATION_ID, any<Notification>()) }
        val notification = notificationSlot.captured
        assertEquals(NotificationConstants.SYNC_CHANNEL_ID, notification.channelId)
        assertEquals("Uploading trees (3/10)", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(context.getString(R.string.syncing), notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals(3, notification.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(10, notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertFalse(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
    }

    @Test
    fun `sync and worker notifications use distinct ids and channels`() {
        assertNotEquals(NotificationConstants.SYNC_NOTIFICATION_ID, NotificationConstants.WORKER_NOTIFICATION_ID)
        assertNotEquals(NotificationConstants.SYNC_CHANNEL_ID, NotificationConstants.WORKER_CHANNEL_ID)
    }
}