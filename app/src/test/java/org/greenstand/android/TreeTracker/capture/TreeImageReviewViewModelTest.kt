package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TreeImageReviewViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = mockk<TreeCapturer>()
    private val userRepo = mockk<UserRepo>()

    private lateinit var treeImageReviewViewModel: TreeImageReviewViewModel


    @Before
    fun setUp() {
        treeImageReviewViewModel = TreeImageReviewViewModel(treeCapturer, userRepo)
    }

    @Test
    fun `updateNote should update state`() = runBlockingTest {
        val note = "note"
        treeImageReviewViewModel.updateNote(note)
        assertEquals(note, treeImageReviewViewModel.state.getOrAwaitValueTest().note)
    }

    @Test
    fun `updateNote review tutorial dialog, returns true`() = runBlockingTest {
        treeImageReviewViewModel.updateReviewTutorialDialog(true)
        treeImageReviewViewModel.state.getOrAwaitValueTest().showReviewTutorial?.let { assert(it) { "${treeImageReviewViewModel.state.getOrAwaitValueTest().showReviewTutorial}" } }
    }

    @Test
    fun `updateNote review tutorial dialog, returns false`() = runBlockingTest {
        treeImageReviewViewModel.updateReviewTutorialDialog(false)
        assert(!treeImageReviewViewModel.state.getOrAwaitValueTest().showReviewTutorial!!) { "${treeImageReviewViewModel.state.getOrAwaitValueTest().showReviewTutorial}" }
    }

    @Test
    fun `addNote setsNote And ClosesDialog, returns false`() = runBlockingTest {
        treeImageReviewViewModel.addNote()
        assert(!treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen) { "${treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen}" }
    }

    @Test
    fun `set Dialog state, check if dialog is open, returns true`() = runBlockingTest {
        treeImageReviewViewModel.setDialogState(true)
        assert(treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen) { "${treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen}" }
    }

    @Test
    fun `set Dialog state, check if dialog is closed, returns false`() = runBlockingTest {
        treeImageReviewViewModel.setDialogState(false)
        assert(!treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen) { "${treeImageReviewViewModel.state.getOrAwaitValueTest().isDialogOpen}" }
    }

}