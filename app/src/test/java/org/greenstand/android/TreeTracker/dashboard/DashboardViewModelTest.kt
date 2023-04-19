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
package org.greenstand.android.TreeTracker.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DashboardViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO
    @MockK(relaxed = true)
    private lateinit var workManager: WorkManager
    @MockK(relaxed = true)
    private lateinit var analytics: Analytics
    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper
    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo
    @MockK(relaxed = true)
    private lateinit var messagesRepo: MessagesRepo
    @MockK(relaxed = true)
    private lateinit var checkForInternetUseCase: CheckForInternetUseCase
    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer
    private lateinit var testSubject: DashboardViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { analytics.syncButtonTapped(any(), any(), any()) } just Runs
        coEvery { dao.getUploadedLegacyTreeImageCount() } returns 3
        coEvery { dao.getUploadedTreeImageCount() } returns 5
        coEvery { dao.getNonUploadedLegacyTreeCaptureImageCount() } returns 2
        coEvery { dao.getNonUploadedTreeImageCount() } returns 4
        coEvery { checkForInternetUseCase.execute(Unit) } returns true
        coEvery { messagesRepo.syncMessages() } just Runs
        coEvery { treesToSyncHelper.getTreeCountToSync() } returns 6
        coEvery { orgRepo.getOrgs() } returns FakeFileGenerator.fakeOrganizationList
        coEvery { messagesRepo.checkForUnreadMessages() } returns false
        every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()
        testSubject = DashboardViewModel(
            dao = dao,
            workManager = workManager,
            analytics = analytics,
            treesToSyncHelper = treesToSyncHelper,
            orgRepo = orgRepo,
            messagesRepo = messagesRepo,
            checkForInternetUseCase = checkForInternetUseCase,
            locationDataCapturer = locationDataCapturer
        )
    }
    @Test
    fun `syncMessages should call syncMessages on messagesRepo if there is internet connection`() = runBlocking {
        coEvery { checkForInternetUseCase.execute(Unit) } returns true
        coEvery { messagesRepo.syncMessages() } just Runs
        testSubject.syncMessages()
        coVerify { messagesRepo.syncMessages() }
    }
    @Test
    fun `syncMessages should not call syncMessages on messagesRepo if there is no internet connection`() = runBlocking {
        coEvery { checkForInternetUseCase.execute(Unit) } returns false
        testSubject.syncMessages()
        coVerify(exactly = 0) { messagesRepo.syncMessages() }
    }
    @Test
    fun `updateData should update the state with correct values querying totalTreesToSync`() = runBlocking {
        val treesToSyncResult = testSubject.state.getOrAwaitValueTest().totalTreesToSync
        assertEquals(treesToSyncResult, 6)
    }

    @Test
    fun `sync should start sync if not syncing and there are trees to sync`() = runBlocking {
        coEvery { checkForInternetUseCase.execute(Unit) } returns true
        testSubject.sync()
        coVerify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        coVerify { analytics.syncButtonTapped(8, 6, 6) }
    }
}