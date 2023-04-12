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
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
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

        val state = treeHeightSelectionViewModel.state.getOrAwaitValueTest()
        val result = state.colors.indexOf(state.selectedColor)

        verify { treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, result.toString()) }
        confirmVerified(treeCapturer)
        Assert.assertEquals(result, 0)
    }
}