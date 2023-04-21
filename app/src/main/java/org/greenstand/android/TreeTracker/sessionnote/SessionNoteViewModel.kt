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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager

data class SessionNoteState(
    val note: String = "",
    val userImagePath: String = "",
)

class SessionNoteViewModel : ViewModel() {

    private val _state = MutableStateFlow(SessionNoteState())
    val state: Flow<SessionNoteState> = _state

    init {
        viewModelScope.launch {
            _state.value = SessionNoteState(
                userImagePath = CaptureSetupScopeManager.getData().user!!.photoPath,
            )
        }
    }

    fun updateNote(note: String) {
        CaptureSetupScopeManager.getData().sessionNote = note
        _state.value = _state.value!!.copy(
            note = note
        )
    }
}