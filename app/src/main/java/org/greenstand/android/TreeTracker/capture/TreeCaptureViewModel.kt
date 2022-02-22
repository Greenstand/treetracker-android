package org.greenstand.android.TreeTracker.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.koin.core.KoinComponent
import org.koin.core.get

data class TreeCaptureState(
    val profilePicUrl: String,
    val isGettingLocation: Boolean = false,
    val isCreatingFakeTrees: Boolean = false,
    val isLocationAvailable: Boolean? = null,
)

class TreeCaptureViewModel(
    profilePicUrl: String,
    private val treeCapturer: TreeCapturer,
    private val sessionTracker: SessionTracker,
    private val createFakeTreesUseCase: CreateFakeTreesUseCase,
    private val locationDataCapturer: LocationDataCapturer,
) : ViewModel() {

    private val _state = MutableLiveData(TreeCaptureState(profilePicUrl))
    val state: LiveData<TreeCaptureState> = _state

    suspend fun captureLocation() {
        _state.value = _state.value?.copy(isGettingLocation = true)
        _state.value = _state.value?.copy(isLocationAvailable = treeCapturer.pinLocation(), isGettingLocation = false)
    }

    fun onImageCaptured(imageFile: File) {
        viewModelScope.launch {
            treeCapturer.setImage(imageFile)
        }
    }

    fun updateBadGpsDialogState(state: Boolean?){
        _state.value = _state.value?.copy(isLocationAvailable = state)
    }

    suspend fun endSession() {
        locationDataCapturer.stopGpsUpdates()
        sessionTracker.endSession()
    }

    suspend fun createFakeTrees() {
        _state.value = _state.value?.copy(isCreatingFakeTrees = true)
        createFakeTreesUseCase.execute(CreateFakeTreesParams(500))
        _state.value = _state.value?.copy(isCreatingFakeTrees = false)
    }

}


class TreeCaptureViewModelFactory(private val profilePicUrl: String)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TreeCaptureViewModel(profilePicUrl, get(), get(), get(), get()) as T
    }
}