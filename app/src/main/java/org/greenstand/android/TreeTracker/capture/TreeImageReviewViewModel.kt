package org.greenstand.android.TreeTracker.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.Users

data class TreeImageReviewState(
    val note: String = "",
    val isDialogOpen: Boolean = false
)
class TreeImageReviewViewModel(
    private val treeCapturer: TreeCapturer,
    private val users: Users,
) : ViewModel() {
    private val _state = MutableLiveData(TreeImageReviewState())
    val state: LiveData<TreeImageReviewState> = _state

    suspend fun approveImage() {
        treeCapturer.saveTree()
    }

    fun updateNote(note: String) {
        _state.value = _state.value?.copy(note = note)
    }

    fun addNote(){
        viewModelScope.launch {
            treeCapturer.setNote(_state.value!!.note)
            _state.value = _state.value?.copy(isDialogOpen = false)
        }
    }
    fun setDialogState(state: Boolean){
        _state.value = _state.value?.copy(isDialogOpen = state)
    }
}