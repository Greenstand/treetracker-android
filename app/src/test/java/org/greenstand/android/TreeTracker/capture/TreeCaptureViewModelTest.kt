package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import java.io.File
import kotlin.math.exp

@ExperimentalCoroutinesApi
class TreeCaptureViewModelTest {

    /*
     Here the Rule runs coroutines on the main thread
     */

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = Mockito.mock(TreeCapturer::class.java)
    private val userRepo = Mockito.mock(UserRepo::class.java)
    private val sessionTracker = Mockito.mock(SessionTracker::class.java)
    private val createFakeTreesUseCase = Mockito.mock(CreateFakeTreesUseCase::class.java)
    private val locationDataCapturer = Mockito.mock(LocationDataCapturer::class.java)

    private lateinit var treeCaptureViewModel: TreeCaptureViewModel

    /*
     Setting the main dispatcher to Dispatcher.Unconfined and
     initializing the treeCaptureViewModel object.
     */

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        treeCaptureViewModel = TreeCaptureViewModel("", userRepo, treeCapturer, sessionTracker, createFakeTreesUseCase, locationDataCapturer)
    }

    /*
     Reset the main dispatcher after each test.
     */

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /*
     Test the image captured by calling setImage on treeCapturer.
     */

    @Test
    fun `test to see image captured`() = runBlockingTest {
        val imageFile: File
        imageFile = File("")
        treeCaptureViewModel.onImageCaptured(imageFile)
        verify(treeCapturer).setImage(imageFile)
    }

    /*
     Test if the state of GPS changes the state with the provided boolean.
     Here it can be true or false that is why we have tests for both cases.

     This will return true.
     */

    @Test
    fun `test to see if gps state, returns true`() = runBlockingTest {
        treeCaptureViewModel.updateBadGpsDialogState(true)
        assertEquals(true, treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable)
    }

    /*
     Test case explained on line 80 and 81.
     This will return false.
     */

    @Test
    fun `test to see if gps state, returns false`() = runBlockingTest {
        treeCaptureViewModel.updateBadGpsDialogState(false)
        assertEquals(false, treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable)
    }

    /*
     Test if the update capture tutorial dialog works with the state provided boolean.
     Here it can be true or false that is why we have tests for both cases.

     This will return true.
     */

    @Test
    fun `test to see if update capture works, returns true`() = runBlockingTest {
        treeCaptureViewModel.updateCaptureTutorialDialog(true)
        assertEquals(true, treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial)
    }

    /*
     Test case explained on line 104 and 105. This will return false now.
     */

    @Test
    fun `test to see if update capture works, returns false`() = runBlockingTest {
        treeCaptureViewModel.updateCaptureTutorialDialog(false)
        assertEquals(false, treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial)
    }

    /*
     Test if createFakeTree() creates fake trees and isCreatingFakeTrees state to false.
     This doesn't have both cases, so it is just false.
     */

    @Test
    fun `test to create fake trees`() = runBlockingTest {
        treeCaptureViewModel.createFakeTrees()
        assertEquals(false, treeCaptureViewModel.state.getOrAwaitValueTest().isCreatingFakeTrees)
    }
}