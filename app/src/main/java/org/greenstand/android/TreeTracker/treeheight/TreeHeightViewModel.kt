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

import androidx.compose.material.ButtonColors
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.view.AppButtonColors

data class TreeHeightSelectionState(
    val colors: List<ButtonColors> = listOf(
        AppButtonColors.ProgressGreen,
        AppButtonColors.MessagePurple,
        AppButtonColors.Yellow,
        AppButtonColors.SkyBlue,
        AppButtonColors.UploadOrange
    ),
    val selectedColor: ButtonColors? = null,
)

class TreeHeightSelectionViewModel(
    private val treeCapturer: TreeCapturer,
) : ViewModel() {

    private val _state = MutableLiveData(TreeHeightSelectionState())
    val state: LiveData<TreeHeightSelectionState> = _state

    fun selectColor(color: ButtonColors) {
        // TODO add proper color values
        val colorIndex = _state.value?.colors?.indexOf(color) ?: -1
        treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, colorIndex.toString())
        _state.value = TreeHeightSelectionState(selectedColor = color)
    }
}