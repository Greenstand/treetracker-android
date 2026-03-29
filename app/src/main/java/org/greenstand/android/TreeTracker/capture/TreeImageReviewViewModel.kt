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
package org.greenstand.android.TreeTracker.capture

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.organization.FeatureResolver
import org.greenstand.android.TreeTracker.models.organization.OrgFeature
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class TreeImageReviewState(
    val treeImagePath: String? = null,
    val note: String = "",
    val isDialogOpen: Boolean = false,
    val showReviewTutorial: Boolean? = null,
    val canNavigateForward: Boolean = false,
)

sealed class TreeImageReviewAction : Action {
    data class UpdateNote(
        val note: String,
    ) : TreeImageReviewAction()

    object CheckIfCanNavigateForward : TreeImageReviewAction()

    data class UpdateReviewTutorialDialog(
        val show: Boolean,
    ) : TreeImageReviewAction()

    object AddNote : TreeImageReviewAction()

    data class SetDialogState(
        val isOpen: Boolean,
    ) : TreeImageReviewAction()

    object NavigateBack : TreeImageReviewAction()
}

class TreeImageReviewViewModel(
    private val treeCapturer: TreeCapturer,
    private val userRepo: UserRepo,
    featureResolver: FeatureResolver,
) : BaseViewModel<TreeImageReviewState, TreeImageReviewAction>(TreeImageReviewState()) {
    private val forceNote =
        featureResolver.isCaptureFlowFeatureEnabled(
            RouteRegistry.ROUTE_TREE_IMAGE_REVIEW,
            OrgFeature.FORCE_NOTE,
        )

    init {
        viewModelScope.launch(Dispatchers.Main) {
            val firstTrack = isFirstTrack()
            updateState {
                copy(
                    showReviewTutorial = firstTrack,
                    treeImagePath = treeCapturer.currentTree?.photoPath,
                )
            }
        }
    }

    override fun handleAction(action: TreeImageReviewAction) {
        when (action) {
            is TreeImageReviewAction.UpdateNote -> {
                updateState { copy(note = action.note) }
            }
            is TreeImageReviewAction.CheckIfCanNavigateForward -> {
                if (forceNote && currentState.note.isBlank()) {
                    updateState { copy(isDialogOpen = true) }
                } else {
                    updateState { copy(canNavigateForward = true) }
                }
            }
            is TreeImageReviewAction.UpdateReviewTutorialDialog -> {
                updateState { copy(showReviewTutorial = action.show) }
            }
            is TreeImageReviewAction.AddNote -> {
                viewModelScope.launch {
                    treeCapturer.setNote(currentState.note)
                    updateState { copy(isDialogOpen = false) }
                }
            }
            is TreeImageReviewAction.SetDialogState -> {
                updateState { copy(isDialogOpen = action.isOpen) }
            }
            else -> { }
        }
    }

    private suspend fun isFirstTrack(): Boolean = userRepo.getPowerUser()?.numberOfTrees?.let { it < 1 } ?: true
}