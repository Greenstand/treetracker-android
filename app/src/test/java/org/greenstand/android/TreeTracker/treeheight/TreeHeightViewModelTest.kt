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
    fun setup(){
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
}