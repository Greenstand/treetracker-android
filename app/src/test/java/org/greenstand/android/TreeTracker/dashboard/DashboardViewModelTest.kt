package org.greenstand.android.TreeTracker.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var treeTrackerDAO: TreeTrackerDAO

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

    private lateinit var dashBoardViewModel: DashboardViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        coEvery { treeTrackerDAO.getUploadedLegacyTreeImageCount() } returns 2
        coEvery { treeTrackerDAO.getUploadedTreeImageCount() } returns 2
        coEvery { treeTrackerDAO.getNonUploadedLegacyTreeCaptureImageCount() } returns 2
        coEvery { treeTrackerDAO.getNonUploadedTreeImageCount() } returns 2
        coEvery { treesToSyncHelper.getTreeCountToSync() } returns 2
        coEvery { orgRepo.getOrgs().size } returns 3
        coEvery { messagesRepo.checkForUnreadMessages() } returns false

        dashBoardViewModel = DashboardViewModel(
            dao = treeTrackerDAO,
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
    fun `WHEN internet connection is true THEN messages will be synced`() = runBlocking {
        coEvery { checkForInternetUseCase.execute(Unit) } returns true

        dashBoardViewModel.syncMessages()

        coVerify(exactly = 1) { messagesRepo.syncMessages() }
    }

    @Test
    fun `WHEN internet connection is false  THEN messages will never be synced`() = runBlocking {
        coEvery { checkForInternetUseCase.execute(Unit) } returns false

        dashBoardViewModel.syncMessages()

        coVerify(exactly = 0) { messagesRepo.syncMessages() }
    }

    @Test
    fun `check state is updated`() = runBlocking {

        val dashboardState = DashboardState(
            treesSynced = 8,
            treesRemainingToSync = 8,
            totalTreesToSync = 8,
            isOrgButtonEnabled = true,
            showUnreadMessageNotification = false
        )

        coEvery { treeTrackerDAO.getUploadedLegacyTreeImageCount() } returns 4
        coEvery { treeTrackerDAO.getUploadedTreeImageCount() } returns 4
        coEvery { treeTrackerDAO.getNonUploadedLegacyTreeCaptureImageCount() } returns 4
        coEvery { treeTrackerDAO.getNonUploadedTreeImageCount() } returns 4
        coEvery { treesToSyncHelper.getTreeCountToSync() } returns 8
        coEvery { orgRepo.getOrgs().size } returns 3
        coEvery { messagesRepo.checkForUnreadMessages() } returns false

        val updateData = dashBoardViewModel.javaClass.getDeclaredMethod("updateData")
        updateData.isAccessible = true
        updateData.invoke(dashBoardViewModel)

        assertEquals(dashBoardViewModel.state.getOrAwaitValueTest(),dashboardState)
    }

}