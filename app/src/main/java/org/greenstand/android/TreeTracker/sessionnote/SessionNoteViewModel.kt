package org.greenstand.android.TreeTracker.sessionnote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager

data class SessionNoteState(
    val note: String = "",
    val userImagePath: String = "",
)

class SessionNoteViewModel : ViewModel() {

    private val _state = MutableLiveData<SessionNoteState>()
    val state: LiveData<SessionNoteState> = _state

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