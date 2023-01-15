package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class TreeCaptureViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = mockk<TreeCapturer>()
    private val userRepo = mockk<UserRepo>()
    private val sessionTracker = mockk<SessionTracker>()
    private val createFakeTreesUseCase = mockk<CreateFakeTreesUseCase>()
    private val locationDataCapturer = mockk<LocationDataCapturer>()

    private lateinit var treeCaptureViewModel: TreeCaptureViewModel


    @Before
    fun setUp() {
        treeCaptureViewModel = TreeCaptureViewModel(
            "",
            userRepo,
            treeCapturer,
            sessionTracker,
            createFakeTreesUseCase,
            locationDataCapturer
        )
    }

    @Test
    fun `test to see image captured`() = runBlockingTest {
        val imageFile = File("naphtali/android")
        coEvery {
            treeCapturer.setImage(imageFile)
        } returns Unit
        treeCaptureViewModel.onImageCaptured(imageFile)
        coVerify {
            treeCapturer.setImage(imageFile)
        }
    }

    @Test
    fun `test to see if gps state, returns true`() = runBlockingTest {
        treeCaptureViewModel.updateBadGpsDialogState(true)
        treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable?.let {
            assert(
                it
            ) { "${treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable}" }
        }
    }

    @Test
    fun `test to see if gps state, returns false`() = runBlockingTest {
        treeCaptureViewModel.updateBadGpsDialogState(false)
        assert(!treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable!!) {"${treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable}"}
    }

    @Test
    fun `test to see if update capture works, returns true`() = runBlockingTest {
        treeCaptureViewModel.updateCaptureTutorialDialog(true)
        treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial?.let { assert(it) {"${treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial}"} }
    }

    @Test
    fun `test to see if update capture works, returns false`() = runBlockingTest {
        treeCaptureViewModel.updateCaptureTutorialDialog(false)
        assert(!treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial!!) {"${treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial}"}
    }
}