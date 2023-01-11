package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.utils.emptyUser
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class TreeCaptureViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val profilePicUrl = ""

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    @MockK(relaxed = true)
    private lateinit var treeCapturer: TreeCapturer

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    @MockK(relaxed = true)
    private lateinit var createFakeTreesUseCase: CreateFakeTreesUseCase

    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer

    private lateinit var treeCaptureViewModel: TreeCaptureViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { userRepo.getPowerUser() } returns emptyUser

        treeCaptureViewModel = TreeCaptureViewModel(
            profilePicUrl = profilePicUrl,
            userRepo = userRepo,
            treeCapturer = treeCapturer,
            sessionTracker = sessionTracker,
            createFakeTreesUseCase = createFakeTreesUseCase,
            locationDataCapturer = locationDataCapturer
        )
    }

    @Test
    fun isFirstTrack() = runBlocking {
        coEvery { userRepo.getPowerUser() } returns User(
            id = 5,
            wallet = "",
            numberOfTrees = 5,
            firstName = "",
            lastName = "",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false
        )

        val result = treeCaptureViewModel.isFirstTrack()
        assertFalse(result)
    }

    @Test
    fun captureLocation() = runBlocking {
        coEvery { treeCapturer.pinLocation() } returns true

        treeCaptureViewModel.captureLocation()

        assertTrue(treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable ?: false)
        assertFalse(treeCaptureViewModel.state.getOrAwaitValueTest().isGettingLocation)
    }

    @Test
    fun updateBadGpsDialogState() = runBlocking {
        treeCaptureViewModel.updateBadGpsDialogState(true)
        assertTrue(treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable ?: false)
    }

    @Test
    fun updateCaptureTutorialDialog() = runBlocking {
        treeCaptureViewModel.updateCaptureTutorialDialog(true)
        assertTrue(treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial ?: false)
    }

    @Test
    fun endSession() = runBlocking {
        treeCaptureViewModel.endSession()

        verify(exactly = 1) { locationDataCapturer.stopGpsUpdates() }
        coVerify(exactly = 1) { sessionTracker.endSession() }
    }

    @Test
    fun createFakeTrees() = runBlocking {
        treeCaptureViewModel.createFakeTrees()

        coVerify(exactly = 1) { createFakeTreesUseCase.execute(CreateFakeTreesParams(50)) }
        assertFalse(treeCaptureViewModel.state.getOrAwaitValueTest().isCreatingFakeTrees)
    }



}