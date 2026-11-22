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
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class SessionNoteState(
    val note: String = "",
    val userImagePath: String = "",
    /** True while session note, restored after process death, reloads its setup user. Next and Back wait. */
    val restoring: Boolean = false,
    /** True when session note came back after process death and its user could not be found again. */
    val setupLost: Boolean = false,
)

sealed class SessionNoteAction : Action {
    data class UpdateNote(
        val note: String,
    ) : SessionNoteAction()

    object NavigateBack : SessionNoteAction()

    object NavigateNext : SessionNoteAction()
}

class SessionNoteViewModel(
    setupRestorer: CaptureSetupRestorer,
) : BaseViewModel<SessionNoteState, SessionNoteAction>(SessionNoteState()) {
    init {
        // After process death Nav3 restores session note with the capture setup scope closed.
        val restoring = setupRestorer.reopenIfClosed()
        if (restoring) updateState { copy(restoring = true) }
        viewModelScope.launch {
            val user = setupRestorer.loadUser(restoring)
            updateState {
                if (user == null) {
                    copy(restoring = false, setupLost = true)
                } else {
                    copy(restoring = false, userImagePath = user.photoPath)
                }
            }
        }
    }

    override fun handleAction(action: SessionNoteAction) {
        // The setup scope is closed once setup is lost; the screen is on its way to the Dashboard.
        if (currentState.setupLost) return
        when (action) {
            is SessionNoteAction.UpdateNote -> {
                CaptureSetupScopeManager.getData().sessionNote = action.note
                updateState { copy(note = action.note) }
            }
            else -> { }
        }
    }
}