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
package org.greenstand.android.TreeTracker.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeoutOrNull
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The dashboard loads its org and unread-message flags on Dispatchers.IO. That work must not
 * come back through Dispatchers.Main: a test that ends first resets Main while the IO thread is
 * dispatching onto it ("Dispatchers.Main is used concurrently with setting it"), and the
 * exception is reported in whichever test runs next.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DashboardViewModelMainDispatcherTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Never advanced: anything dispatched to Main in this test does not run.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule(StandardTestDispatcher())

    @Test
    fun `non-tree data reaches the state without a turn of the main dispatcher`() {
        val dao = mockk<TreeTrackerDAO>(relaxed = true)
        every { dao.getUploadedLegacyTreeImageCountFlow() } returns flowOf(0)
        every { dao.getUploadedTreeImageCountFlow() } returns flowOf(0)
        every { dao.getNonUploadedLegacyTreeCaptureImageCountFlow() } returns flowOf(0)
        every { dao.getNonUploadedTreeImageCountFlow() } returns flowOf(0)
        val orgRepo = mockk<OrgRepo>(relaxed = true)
        coEvery { orgRepo.getOrgs() } returns listOf(mockk(), mockk())
        val messagesRepo = mockk<MessagesRepo>(relaxed = true)
        coEvery { messagesRepo.checkForUnreadMessages() } returns true

        val viewModel =
            DashboardViewModel(
                dao = dao,
                workManager = mockk(relaxed = true),
                analytics = mockk(relaxed = true),
                treesToSyncHelper = mockk(relaxed = true),
                orgRepo = orgRepo,
                messagesRepo = messagesRepo,
                checkForInternetUseCase = mockk(relaxed = true),
                locationDataCapturer = mockk(relaxed = true),
            )

        val loaded =
            runBlocking {
                withTimeoutOrNull(10_000) { viewModel.state.first { it.isOrgButtonEnabled } }
            }
        assertNotNull("The org and message flags never reached the state: they waited for Dispatchers.Main", loaded)
        assertTrue(loaded!!.showUnreadMessageNotification)
    }
}