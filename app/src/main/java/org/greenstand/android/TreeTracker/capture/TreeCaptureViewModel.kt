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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.utils.updateState
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
    val forceImageScaling: Boolean = false,
    val imageScalingHeight: Int = 1920,
)

class TreeCaptureViewModel(
    profilePicUrl: String,
    private val userRepo: UserRepo,
    private val treeCapturer: TreeCapturer,
    private val sessionTracker: SessionTracker,
    private val createFakeTreesUseCase: CreateFakeTreesUseCase,
    private val locationDataCapturer: LocationDataCapturer,
    private val configurator: Configurator,
) : ViewModel() {

    private val _state = MutableLiveData(TreeCaptureState(profilePicUrl))
    val state: LiveData<TreeCaptureState> = _state

    init {
        viewModelScope.launch(Dispatchers.Main) {
            _state.updateState {
                val enabled = configurator.getBoolean(ConfigKeys.FORCE_IMAGE_SIZE)
                val imageHeight = configurator.getInt(ConfigKeys.IMAGE_CAPTURE_HEIGHT)
                copy(
                    showCaptureTutorial = isFirstTrack(),
                    forceImageScaling = enabled,
                    imageScalingHeight = imageHeight,
                )
            }
        }
    }

    suspend fun captureLocation() {
        _state.value = _state.value?.copy(isGettingLocation = true)
        _state.value = _state.value?.copy(isLocationAvailable = treeCapturer.pinLocation(), isGettingLocation = false)
    }

    fun onImageCaptured(imageFile: File) {
        treeCapturer.setImage(imageFile)
    }

    fun updateBadGpsDialogState(state: Boolean?) {
        _state.value = _state.value?.copy(isLocationAvailable = state)
    }

    fun updateCaptureTutorialDialog(state: Boolean) {
        _state.value = _state.value?.copy(showCaptureTutorial = state)
    }

    suspend fun isFirstTrack(): Boolean = userRepo.getPowerUser()!!.numberOfTrees < 1

    suspend fun createFakeTrees() {
        _state.value = _state.value?.copy(isCreatingFakeTrees = true)
        createFakeTreesUseCase.execute(CreateFakeTreesParams(50))
        _state.value = _state.value?.copy(isCreatingFakeTrees = false)
    }
}

class TreeCaptureViewModelFactory(private val profilePicUrl: String) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        CaptureFlowScopeManager.open()
        return TreeCaptureViewModel(profilePicUrl, get(), get(), get(), get(), get(), get()) as T
    }
}