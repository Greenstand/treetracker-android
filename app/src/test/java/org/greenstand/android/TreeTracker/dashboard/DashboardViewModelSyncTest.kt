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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.background.NotificationConstants
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.viewmodel.ShowSnackbar
import org.greenstand.android.TreeTracker.viewmodel.TextRef
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the dashboard's sync button, WorkManager state observation (snackbars), tree count
 * bookkeeping during a sync, org/message flags and lifecycle cleanup.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DashboardViewModelSyncTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val dao = mockk<TreeTrackerDAO>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val analytics = mockk<Analytics>(relaxed = true)
    private val treesToSyncHelper = mockk<TreesToSyncHelper>(relaxed = true)
    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)

    private val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
    private val nonUploadedTrees = MutableStateFlow(4)

    @Before
    fun setUp() {
        mockkObject(FeatureFlags)
        every { FeatureFlags.DEBUG_ENABLED } returns false
        // 3 + 5 = 8 synced, 2 + 4 = 6 remaining
        every { dao.getUploadedLegacyTreeImageCountFlow() } returns flowOf(3)
        every { dao.getUploadedTreeImageCountFlow() } returns flowOf(5)
        every { dao.getNonUploadedLegacyTreeCaptureImageCountFlow() } returns flowOf(2)
        every { dao.getNonUploadedTreeImageCountFlow() } returns nonUploadedTrees
        every { workManager.getWorkInfosForUniqueWorkLiveData(any()) } returns workInfoLiveData
        every { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()
        coEvery { orgRepo.getOrgs() } returns FakeFileGenerator.fakeOrganizationList
        coEvery { messagesRepo.checkForUnreadMessages() } returns false
    }

    @After
    fun tearDown() {
        unmockkObject(FeatureFlags)
    }

    private fun createViewModel(): DashboardViewModel =
        DashboardViewModel(
            dao = dao,
            workManager = workManager,
            analytics = analytics,
            treesToSyncHelper = treesToSyncHelper,
            orgRepo = orgRepo,
            messagesRepo = messagesRepo,
            checkForInternetUseCase = checkForInternetUseCase,
            locationDataCapturer = locationDataCapturer,
        )

    private fun postWorkState(state: WorkInfo.State) {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns state
        workInfoLiveData.value = listOf(workInfo)
    }

    private fun DashboardViewModel.snackbarMessages(): List<TextRef> = events.replayCache.mapNotNull { (it.getContentIfNotConsumed() as? ShowSnackbar)?.message }

    @Test
    fun `init stops gps updates and observes the unique sync work`() {
        createViewModel()

        verify { locationDataCapturer.stopGpsUpdates() }
        verify { workManager.getWorkInfosForUniqueWorkLiveData(NotificationConstants.UNIQUE_WORK_ID) }
        assertTrue(workInfoLiveData.hasObservers())
    }

    @Test
    fun `init publishes tree counts and upload progress`() =
        runTest {
            val vm = createViewModel()

            val state = vm.state.first { it.totalTreesToSync != 0 }
            assertEquals(8, state.treesSynced)
            assertEquals(6, state.treesRemainingToSync)
            assertEquals(6, state.totalTreesToSync)
            assertEquals(1f, state.uploadProgress, 0f)
            assertFalse(state.showTreeSyncReminderDialog)
        }

    @Test
    fun `upload progress is zero when there is nothing to sync`() {
        assertEquals(0f, DashboardState().uploadProgress, 0f)
        assertEquals(0.5f, DashboardState(treesRemainingToSync = 3, totalTreesToSync = 6).uploadProgress, 0f)
    }

    @Test
    fun `sync reminder dialog stays hidden just below the threshold`() =
        runTest {
            every { dao.getNonUploadedLegacyTreeCaptureImageCountFlow() } returns flowOf(999)
            every { dao.getNonUploadedTreeImageCountFlow() } returns flowOf(1000)

            val vm = createViewModel()

            val state = vm.state.first { it.treesRemainingToSync == 1999 }
            assertFalse(state.showTreeSyncReminderDialog)
        }

    @Test
    fun `org button is enabled with several orgs and unread messages are flagged`() =
        runTest {
            coEvery { messagesRepo.checkForUnreadMessages() } returns true

            val vm = createViewModel()

            val state = vm.state.first { it.showUnreadMessageNotification }
            assertTrue(state.isOrgButtonEnabled)
        }

    @Test
    fun `org button is disabled with a single org`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns listOf(FakeFileGenerator.fakeOrganizationList.first())
            coEvery { messagesRepo.checkForUnreadMessages() } returns true

            val vm = createViewModel()

            val state = vm.state.first { it.showUnreadMessageNotification }
            assertFalse(state.isOrgButtonEnabled)
        }

    @Test
    fun `Sync with trees enqueues the unique sync work and reports the tap`() {
        val vm = createViewModel()

        vm.handleAction(DashboardAction.Sync)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                NotificationConstants.UNIQUE_WORK_ID,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
        verify { analytics.syncButtonTapped(6, 8, 6) }
        assertTrue(vm.snackbarMessages().isEmpty())
    }

    @Test
    fun `Sync with nothing to sync shows a snackbar and does not enqueue work`() {
        nonUploadedTrees.value = 0
        every { dao.getNonUploadedLegacyTreeCaptureImageCountFlow() } returns flowOf(0)
        val vm = createViewModel()

        vm.handleAction(DashboardAction.Sync)

        assertEquals(listOf<TextRef>(TextRef.Res(R.string.nothing_to_sync)), vm.snackbarMessages())
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        verify { analytics.syncButtonTapped(0, 8, 0) }
    }

    @Test
    fun `Sync in debug builds enqueues work even with nothing to sync`() {
        every { FeatureFlags.DEBUG_ENABLED } returns true
        nonUploadedTrees.value = 0
        every { dao.getNonUploadedLegacyTreeCaptureImageCountFlow() } returns flowOf(0)
        val vm = createViewModel()

        vm.handleAction(DashboardAction.Sync)

        verify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        assertTrue(vm.snackbarMessages().isEmpty())
    }

    @Test
    fun `Sync while a sync is running cancels the work and reports the stop`() {
        val vm = createViewModel()
        postWorkState(WorkInfo.State.RUNNING)

        vm.handleAction(DashboardAction.Sync)

        verify(exactly = 1) { workManager.cancelUniqueWork(NotificationConstants.UNIQUE_WORK_ID) }
        verify { analytics.stopButtonTapped(6, 8, 6) }
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        verify(exactly = 0) { analytics.syncButtonTapped(any(), any(), any()) }
    }

    @Test
    fun `running work shows sync started`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.RUNNING)

        assertEquals(listOf<TextRef>(TextRef.Res(R.string.sync_started)), vm.snackbarMessages())
    }

    @Test
    fun `enqueued work shows sync preparing`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.ENQUEUED)

        assertEquals(listOf<TextRef>(TextRef.Res(R.string.sync_preparing)), vm.snackbarMessages())
    }

    @Test
    fun `blocked work shows sync blocked`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.BLOCKED)

        assertEquals(listOf<TextRef>(TextRef.Res(R.string.sync_blocked)), vm.snackbarMessages())
    }

    @Test
    fun `cancelled work shows sync stopped`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.CANCELLED)

        assertEquals(listOf<TextRef>(TextRef.Res(R.string.sync_stopped)), vm.snackbarMessages())
    }

    @Test
    fun `failed work shows sync failed`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.FAILED)

        assertEquals(listOf<TextRef>(TextRef.Res(R.string.sync_failed)), vm.snackbarMessages())
    }

    @Test
    fun `succeeded work after a running sync shows sync successful`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.RUNNING)
        postWorkState(WorkInfo.State.SUCCEEDED)

        assertEquals(
            listOf<TextRef>(TextRef.Res(R.string.sync_started), TextRef.Res(R.string.sync_successful)),
            vm.snackbarMessages(),
        )
    }

    @Test
    fun `succeeded work from an earlier session shows no snackbar`() {
        val vm = createViewModel()

        postWorkState(WorkInfo.State.SUCCEEDED)

        assertTrue(vm.snackbarMessages().isEmpty())
    }

    @Test
    fun `empty work info list shows no snackbar`() {
        val vm = createViewModel()

        workInfoLiveData.value = emptyList()

        assertTrue(vm.events.replayCache.isEmpty())
    }

    @Test
    fun `after a finished sync the next Sync tap starts a new sync`() {
        val vm = createViewModel()
        postWorkState(WorkInfo.State.RUNNING)
        postWorkState(WorkInfo.State.SUCCEEDED)

        vm.handleAction(DashboardAction.Sync)

        verify(exactly = 1) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `total trees to sync is frozen while syncing so progress can drop`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.totalTreesToSync == 6 }
            postWorkState(WorkInfo.State.RUNNING)

            nonUploadedTrees.value = 3

            val state = vm.state.first { it.treesRemainingToSync == 5 }
            assertEquals(6, state.totalTreesToSync)
            assertEquals(5f / 6f, state.uploadProgress, 0.0001f)
        }

    @Test
    fun `a finished sync refreshes org and message flags`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns listOf(FakeFileGenerator.fakeOrganizationList.first())
            coEvery { messagesRepo.checkForUnreadMessages() } returns true
            val vm = createViewModel()
            vm.state.first { it.showUnreadMessageNotification }
            assertFalse(vm.state.value.isOrgButtonEnabled)

            coEvery { orgRepo.getOrgs() } returns FakeFileGenerator.fakeOrganizationList
            postWorkState(WorkInfo.State.FAILED)

            vm.state.first { it.isOrgButtonEnabled }
            coVerify(atLeast = 2) { orgRepo.getOrgs() }
        }

    @Test
    fun `navigation actions are left to the screen`() {
        // A single org and no unread messages keep the async flag refresh equal to the defaults.
        coEvery { orgRepo.getOrgs() } returns listOf(FakeFileGenerator.fakeOrganizationList.first())
        val vm = createViewModel()
        val initialState = vm.state.value

        listOf(
            DashboardAction.NavigateToOrg,
            DashboardAction.NavigateToCapture,
            DashboardAction.NavigateToMessages,
            DashboardAction.NavigateToSettings,
            DashboardAction.ConfirmSyncReminderDialog,
        ).forEach(vm::handleAction)

        assertEquals(initialState, vm.state.value)
        assertTrue(vm.events.replayCache.isEmpty())
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        coVerify(exactly = 0) { messagesRepo.syncMessages() }
    }

    @Test
    fun `clearing the view model stops observing the sync work`() {
        val vm = createViewModel()
        val store = ViewModelStore()
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = vm as T
            }
        ViewModelProvider(store, factory)[DashboardViewModel::class.java]
        assertTrue(workInfoLiveData.hasObservers())

        store.clear()

        assertFalse(workInfoLiveData.hasObservers())
    }
}