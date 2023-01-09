package org.greenstand.android.TreeTracker.treeheight

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.Tree
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
    fun `selected color is progress green, asserts correct color added `()= runBlocking {
        val color = AppButtonColors.ProgressGreen
        treeHeightSelectionViewModel.selectColor(color)
        treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, color.toString())
        val selectedColorResult = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        val setsColorAttribute = treeHeightSelectionViewModel.state.getOrAwaitValueTest().colours.first()
        Assert.assertEquals(selectedColorResult, AppButtonColors.ProgressGreen)
        Assert.assertEquals(setsColorAttribute, AppButtonColors.ProgressGreen)
    }

    @Test
    fun `selected color is sky blue, assert correct color added`()= runBlocking {
        val color = AppButtonColors.SkyBlue
        treeHeightSelectionViewModel.selectColor(color)
        treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, color.toString())
        val selectedColorResult = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        val setsColorAttribute = treeHeightSelectionViewModel.state.getOrAwaitValueTest().colours[3]
        Assert.assertEquals(selectedColorResult, AppButtonColors.SkyBlue)
        Assert.assertEquals(setsColorAttribute, AppButtonColors.SkyBlue)
    }

    @Test
    fun `selected color is upload orange, assert correct color added`()= runBlocking {
        val color = AppButtonColors.UploadOrange
        treeHeightSelectionViewModel.selectColor(color)
        treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, color.toString())
        val selectedColorResult = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        val setsColorAttribute = treeHeightSelectionViewModel.state.getOrAwaitValueTest().colours[4]
        Assert.assertEquals(selectedColorResult, AppButtonColors.UploadOrange)
        Assert.assertEquals(setsColorAttribute, AppButtonColors.UploadOrange)
    }


    @Test
    fun `selected color is yellow, assert correct color added`()= runBlocking {
        val color = AppButtonColors.Yellow
        treeHeightSelectionViewModel.selectColor(color)
        treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, color.toString())
        val selectedColorResult = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        val setsColorAttribute = treeHeightSelectionViewModel.state.getOrAwaitValueTest().colours[2]
        Assert.assertEquals(selectedColorResult, AppButtonColors.Yellow)
        Assert.assertEquals(setsColorAttribute, AppButtonColors.Yellow)
    }

    @Test
    fun `selected color is message purple, assert correct color added`()= runBlocking {
        val color = AppButtonColors.MessagePurple
        treeHeightSelectionViewModel.selectColor(color)
        treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, color.toString())
        val selectedColorResult = treeHeightSelectionViewModel.state.getOrAwaitValueTest().selectedColour
        val setsColorAttribute = treeHeightSelectionViewModel.state.getOrAwaitValueTest().colours[1]
        Assert.assertEquals(selectedColorResult, AppButtonColors.MessagePurple)
        Assert.assertEquals(setsColorAttribute, AppButtonColors.MessagePurple)
    }
}