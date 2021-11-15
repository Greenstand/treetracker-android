package org.greenstand.android.TreeTracker.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.koin.core.KoinComponent
import org.koin.core.get

data class TreeCaptureState(
    val profilePicUrl: String,
   val isGettingLocation: Boolean = false,
)

class TreeCaptureViewModel(
    profilePicUrl: String,
    private val treeCapturer: TreeCapturer,
) : ViewModel() {

    private val _state = MutableLiveData(TreeCaptureState(profilePicUrl))
    val state: LiveData<TreeCaptureState> = _state

    suspend fun captureLocation() {
        _state.value = _state.value?.copy(isGettingLocation = true)
        treeCapturer.pinLocation()
        _state.value = _state.value?.copy(isGettingLocation = false)
    }

    fun onImageCaptured(imageFile: File) {
        viewModelScope.launch {
            treeCapturer.
            setImage(imageFile)
        }
    }
    fun addNote(note: String){
        viewModelScope.launch {
            treeCapturer.setNote(note)
        }
    }

}


class TreeCaptureViewModelFactory(private val profilePicUrl: String)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TreeCaptureViewModel(profilePicUrl, get()) as T
    }
}