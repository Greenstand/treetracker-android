package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class TreeImageReviewViewModelTest {

    /*
     Here the Rule runs coroutines on the main thread
     */

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = mock(TreeCapturer::class.java)
    private val userRepo = mock(UserRepo::class.java)

    private lateinit var treeImageReviewViewModel: TreeImageReviewViewModel

    /*
     Setting the main dispatcher to Dispatcher.Unconfined and
     initializing the treeImageReviewViewModel object.
     */

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        treeImageReviewViewModel = TreeImageReviewViewModel(treeCapturer, userRepo)
    }

    /*
     Reset the main dispatcher after each test.
     */

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /*
     Test if image is approved by calling saveTree on treeCapturer.
     */

    @Test
    fun `image is being approved`() = runBlockingTest {
        treeImageReviewViewModel.approveImage()
        verify(treeCapturer).saveTree()
    }

    /*
     Test if updateNote updates the state with the provided note.
     */

    @Test
    fun `updateNote should update state`() = runBlockingTest {
        val note = "note"
        treeImageReviewViewModel.updateNote(note)
        assertEquals(note, treeImageReviewViewModel.state.getOrAwaitValueTest().note)
    }

    /*
     Test if updateReviewTutorialDialog updates the state with the provided boolean.
     Here it can be true or false that is why we have tests for both cases.

    This will return tru
     */

    @Test
    fun `updateNote review tutorial dialog, returns true`() = runBlockingTest {
        val state = true
        treeImageReviewViewModel.updateReviewTutorialDialog(true)
        assertEquals(state, treeImageReviewViewModel.state.getOrAwaitValueTest().showReviewTutorial)
    }

    /*
     Test case explained on line 77 and 79. This will return false now.
     */

    @Test
    fun `updateNote review tutorial dialog, returns false`() = runBlockingTest {
        val falseState = false
        treeImageReviewViewModel.updateReviewTutorialDialog(false)
        assertEquals(falseState, treeImageReviewViewModel.state.getOrAwaitValueTest().showReviewTutorial)
    }

    /*
     Test if addNote updates the isDialogOpen state to false.
     This doesn't have both cases, so it is just false.
     */

    @Test
    fun `addNote setsNote And ClosesDialog`() = runBlockingTest {
        treeImageReviewViewModel.addNote()
        assertEquals(false, treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen)
    }

    /*
     Test if setDialogState updates the isDialogOpen state with the provided boolean.
     Here it can be true or false that is why we have tests for both cases.
     */

    @Test
    fun `set Dialog state, check if dialog is open, returns true`() = runBlockingTest {
        val dialogState = true
        treeImageReviewViewModel.setDialogState(dialogState)
        assertEquals(dialogState, treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen)
    }

    /*
     Test case explained on line 111 and 112.
     */

    @Test
    fun `set Dialog state, check if dialog is closed, returns false`() = runBlockingTest {
        val dialogState = false
        treeImageReviewViewModel.setDialogState(dialogState)
        assertEquals(dialogState, treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen)
    }
}