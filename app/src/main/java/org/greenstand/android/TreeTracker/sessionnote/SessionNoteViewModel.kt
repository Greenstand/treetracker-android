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
package org.greenstand.android.TreeTracker.sessionnote

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class SessionNoteState(
    val note: String = "",
    val userImagePath: String = "",
)

sealed class SessionNoteAction : Action {
    data class UpdateNote(val note: String) : SessionNoteAction()
    object NavigateBack : SessionNoteAction()
    object NavigateNext : SessionNoteAction()
}

class SessionNoteViewModel : BaseViewModel<SessionNoteState, SessionNoteAction>(SessionNoteState()) {

    init {
        viewModelScope.launch {
            updateState {
                copy(userImagePath = CaptureSetupScopeManager.getData().user!!.photoPath)
            }
        }
    }

    override fun handleAction(action: SessionNoteAction) {
        when (action) {
            is SessionNoteAction.UpdateNote -> {
                CaptureSetupScopeManager.getData().sessionNote = action.note
                updateState { copy(note = action.note) }
            }
            else -> { }
        }
    }
}
