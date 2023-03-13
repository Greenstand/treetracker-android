package org.greenstand.android.TreeTracker.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

data class TreeCaptureState(
    val profilePicUrl: String,
    val isGettingLocation: Boolean = false,
    val isCreatingFakeTrees: Boolean = false,
    val isLocationAvailable: Boolean? = null,
    val showCaptureTutorial: Boolean? = null,
    val convergencePercentage: Float = 0f,
)

class TreeCaptureViewModel(
    profilePicUrl: String,
    private val userRepo: UserRepo,
    private val treeCapturer: TreeCapturer,
    private val sessionTracker: SessionTracker,
    private val createFakeTreesUseCase: CreateFakeTreesUseCase,
    private val locationDataCapturer: LocationDataCapturer,
) : ViewModel() {

    private val _state = MutableLiveData(TreeCaptureState(profilePicUrl))
    val state: LiveData<TreeCaptureState> = _state

    init {
        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value?.copy(showCaptureTutorial = isFirstTrack() )
        }
    }

    suspend fun captureLocation() {
        _state.value = _state.value?.copy(isGettingLocation = true )
        locationDataCapturer.percentageConvergenceObservers.add { newValue ->
            _state.value = _state.value?.copy( convergencePercentage = newValue )
        }
        _state.value = _state.value?.copy(isLocationAvailable = treeCapturer.pinLocation(), isGettingLocation = false)
    }

    fun onImageCaptured(imageFile: File) {
        treeCapturer.setImage(imageFile)
    }

    fun updateBadGpsDialogState(state: Boolean?){
        _state.value = _state.value?.copy(isLocationAvailable = state)
    }

    fun updateCaptureTutorialDialog(state: Boolean){
        _state.value = _state.value?.copy(showCaptureTutorial = state)
    }

    suspend fun isFirstTrack(): Boolean = userRepo.getPowerUser()!!.numberOfTrees < 1

    suspend fun createFakeTrees() {
        _state.value = _state.value?.copy(isCreatingFakeTrees = true)
        createFakeTreesUseCase.execute(CreateFakeTreesParams(50))
        _state.value = _state.value?.copy(isCreatingFakeTrees = false)
    }

}


class TreeCaptureViewModelFactory(private val profilePicUrl: String)
    : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        CaptureFlowScopeManager.open()
        return TreeCaptureViewModel(profilePicUrl, get(), get(), get(), get(),get()) as T
    }
}