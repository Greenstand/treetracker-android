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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo

data class TreeImageReviewState(
    val treeImagePath: String? = null,
    val note: String = "",
    val isDialogOpen: Boolean = false,
    val showReviewTutorial: Boolean? = null
)
class TreeImageReviewViewModel(
    private val treeCapturer: TreeCapturer,
    private val userRepo: UserRepo,
) : ViewModel() {
    private val _state = MutableLiveData(TreeImageReviewState())
    val state: LiveData<TreeImageReviewState> = _state

    init {
        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value?.copy(
                showReviewTutorial = isFirstTrack(),
                treeImagePath = treeCapturer.currentTree!!.photoPath,
            )
        }
    }

    fun updateNote(note: String) {
        _state.value = _state.value?.copy(note = note)
    }

    fun updateReviewTutorialDialog(state: Boolean) {
        _state.value = _state.value?.copy(showReviewTutorial = state)
    }

    fun addNote() {
        viewModelScope.launch {
            treeCapturer.setNote(_state.value!!.note)
            _state.value = _state.value?.copy(isDialogOpen = false)
        }
    }

    suspend fun isFirstTrack(): Boolean = userRepo.getPowerUser()!!.numberOfTrees < 1

    fun setDialogState(state: Boolean) {
        _state.value = _state.value?.copy(isDialogOpen = state)
    }
}