package org.greenstand.android.TreeTracker.treeheight

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TreeHeightViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = mockk<TreeCapturer>(relaxed = true)
    private lateinit var treeHeightSelectionViewModel: TreeHeightSelectionViewModel

    @Before
    fun setupViewModel(){
        treeHeightSelectionViewModel = TreeHeightSelectionViewModel(treeCapturer)
    }

    @Test
    fun `selected color is progress green, assert correct color`()= runBlocking {
        treeHeightSelectionViewModel.selectColor(AppButtonColors.ProgressGreen)
        val result = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        Assert.assertEquals(result, AppButtonColors.ProgressGreen)
    }

    @Test
    fun `selected color is sky blue, assert correct color`()= runBlocking {
        treeHeightSelectionViewModel.selectColor(AppButtonColors.SkyBlue)
        val result = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        Assert.assertEquals(result, AppButtonColors.SkyBlue)
    }

    @Test
    fun `selected color is upload orange, assert correct color`()= runBlocking {
        treeHeightSelectionViewModel.selectColor(AppButtonColors.UploadOrange)
        val result = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        Assert.assertEquals(result,AppButtonColors.UploadOrange)
    }


    @Test
    fun `selected color is yellow, assert correct color`()= runBlocking {
        treeHeightSelectionViewModel.selectColor(AppButtonColors.Yellow)
        val result = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        Assert.assertEquals(result, AppButtonColors.Yellow)
    }

    @Test
    fun `selected color is message purple, assert correct color`()= runBlocking {
        treeHeightSelectionViewModel.selectColor(AppButtonColors.MessagePurple)
        val result = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        Assert.assertEquals(result, AppButtonColors.MessagePurple)
    }
}