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

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TreeSyncWorkerForegroundInfoTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `worker notification is titled with the app label and opens the main activity`() =
        runTest {
            val worker = TestListenableWorkerBuilder<TreeSyncWorker>(context).build()

            val notification = worker.getForegroundInfo().notification

            val appLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
            assertEquals(appLabel, notification.extras.getString(Notification.EXTRA_TITLE))
            val intent = shadowOf(notification.contentIntent).savedIntent
            assertEquals(NotificationConstants.MAIN_ACTIVITY_CLASS, intent.component?.className)
            assertEquals(context.packageName, intent.component?.packageName)
        }
}