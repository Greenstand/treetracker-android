/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.treeheight

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun setup() {
        treeHeightSelectionViewModel = TreeHeightSelectionViewModel(treeCapturer)
    }

    @Test
    fun `selected color is progress green, asserts correct color added `() =
        runTest {
            val color = AppButtonColors.ProgressGreen
            treeHeightSelectionViewModel.handleAction(TreeHeightAction.SelectColor(color))

            val state = treeHeightSelectionViewModel.state.value
            val result = state.colors.indexOf(state.selectedColor)

            verify { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, result.toString()) }
            confirmVerified(treeCapturer)
            Assert.assertEquals(result, 0)
        }

    @Test
    fun `initial state offers five height colors with nothing selected`() {
        val state = treeHeightSelectionViewModel.state.value

        assertEquals(
            listOf(
                AppButtonColors.ProgressGreen,
                AppButtonColors.MessagePurple,
                AppButtonColors.Yellow,
                AppButtonColors.SkyBlue,
                AppButtonColors.UploadOrange,
            ),
            state.colors,
        )
        assertNull(state.selectedColor)
        assertTrue(treeHeightSelectionViewModel.events.replayCache.isEmpty())
    }

    @Test
    fun `each color is stored as its index in the color list`() {
        val colors = treeHeightSelectionViewModel.state.value.colors

        colors.forEachIndexed { index, color ->
            treeHeightSelectionViewModel.handleAction(TreeHeightAction.SelectColor(color))

            verify { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, index.toString()) }
            assertEquals(color, treeHeightSelectionViewModel.state.value.selectedColor)
        }
    }

    @Test
    fun `selecting another color replaces the selection and keeps the color list`() {
        treeHeightSelectionViewModel.handleAction(TreeHeightAction.SelectColor(AppButtonColors.ProgressGreen))
        treeHeightSelectionViewModel.handleAction(TreeHeightAction.SelectColor(AppButtonColors.UploadOrange))

        val state = treeHeightSelectionViewModel.state.value
        assertEquals(AppButtonColors.UploadOrange, state.selectedColor)
        assertEquals(5, state.colors.size)
        verify(exactly = 1) { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, "0") }
        verify(exactly = 1) { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, "4") }
    }

    @Test
    fun `a color outside the height list is stored as index -1`() {
        treeHeightSelectionViewModel.handleAction(TreeHeightAction.SelectColor(AppButtonColors.DeclineRed))

        verify { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, "-1") }
        assertEquals(AppButtonColors.DeclineRed, treeHeightSelectionViewModel.state.value.selectedColor)
    }

    @Test
    fun `selecting a color emits no ui event`() {
        treeHeightSelectionViewModel.handleAction(TreeHeightAction.SelectColor(AppButtonColors.Yellow))

        assertTrue(treeHeightSelectionViewModel.events.replayCache.isEmpty())
    }
}