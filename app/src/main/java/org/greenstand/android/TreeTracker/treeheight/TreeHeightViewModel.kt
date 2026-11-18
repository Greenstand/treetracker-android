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
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.view.AppButtonColors
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.greenstand.android.TreeTracker.viewmodel.NavigationEvent

data class TreeHeightSelectionState(
    val colors: List<ButtonColors> =
        listOf(
            AppButtonColors.ProgressGreen,
            AppButtonColors.MessagePurple,
            AppButtonColors.Yellow,
            AppButtonColors.SkyBlue,
            AppButtonColors.UploadOrange,
        ),
    val selectedColor: ButtonColors? = null,
)

sealed class TreeHeightAction : Action {
    data class SelectColor(
        val color: ButtonColors,
    ) : TreeHeightAction()
}

class TreeHeightSelectionViewModel(
    private val treeCapturer: TreeCapturer,
    // True when the capture flow was rebuilt after Android killed the app on this screen.
    restoredAfterProcessDeath: Boolean = false,
) : BaseViewModel<TreeHeightSelectionState, TreeHeightAction>(TreeHeightSelectionState()) {
    init {
        if (restoredAfterProcessDeath && treeCapturer.currentTree == null) {
            // The tree could not be restored, so there is nothing to measure.
            sendEvent(NavigationEvent { CaptureFlowScopeManager.nav.goToDashboard(this) })
        }
    }

    override fun handleAction(action: TreeHeightAction) {
        when (action) {
            is TreeHeightAction.SelectColor -> {
                val colorIndex = currentState.colors.indexOf(action.color)
                treeCapturer.addAttribute(Tree.TREE_COLOR_ATTR_KEY, colorIndex.toString())
                updateState { TreeHeightSelectionState(selectedColor = action.color) }
            }
        }
    }
}