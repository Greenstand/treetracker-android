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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.greenstand.android.TreeTracker.viewmodel.NavigationEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber
import java.io.File

data class TreeCaptureState(
    val profilePicUrl: String = "",
    val isGettingLocation: Boolean = false,
    val isCreatingFakeTrees: Boolean = false,
    val isLocationAvailable: Boolean? = null,
    val showCaptureTutorial: Boolean? = null,
    val convergencePercentage: Float = 0f,
    val forceImageScaling: Boolean = false,
    val imageScalingHeight: Int = 1920,
)

sealed class TreeCaptureAction : Action {
    object CaptureLocation : TreeCaptureAction()

    data class OnImageCaptured(
        val imageFile: File,
    ) : TreeCaptureAction()

    data class UpdateBadGpsDialogState(
        val state: Boolean?,
    ) : TreeCaptureAction()

    data class UpdateCaptureTutorialDialog(
        val show: Boolean,
    ) : TreeCaptureAction()

    object CreateFakeTrees : TreeCaptureAction()
}

class TreeCaptureViewModel(
    profilePicUrl: String,
    private val userRepo: UserRepo,
    private val treeCapturer: TreeCapturer,
    private val createFakeTreesUseCase: CreateFakeTreesUseCase,
    private val configurator: Configurator,
) : BaseViewModel<TreeCaptureState, TreeCaptureAction>(TreeCaptureState(profilePicUrl = profilePicUrl)) {
    private var pinLocationDeferred: CompletableDeferred<Boolean>? = null

    init {
        viewModelScope.launch(Dispatchers.Main) {
            val enabled = configurator.getBoolean(ConfigKeys.FORCE_IMAGE_SIZE)
            val imageHeight = configurator.getInt(ConfigKeys.IMAGE_CAPTURE_HEIGHT)
            val firstTrack = isFirstTrack()
            updateState {
                copy(
                    showCaptureTutorial = firstTrack,
                    forceImageScaling = enabled,
                    imageScalingHeight = imageHeight,
                )
            }
        }
    }

    override fun handleAction(action: TreeCaptureAction) {
        when (action) {
            is TreeCaptureAction.CaptureLocation -> {
                if (pinLocationDeferred != null) return
                val deferred = CompletableDeferred<Boolean>()
                pinLocationDeferred = deferred
                viewModelScope.launch {
                    updateState { copy(isGettingLocation = true) }
                    val locationAvailable = treeCapturer.pinLocation()
                    deferred.complete(locationAvailable)
                    updateState { copy(isLocationAvailable = locationAvailable, isGettingLocation = false) }
                }
            }
            is TreeCaptureAction.OnImageCaptured -> {
                viewModelScope.launch {
                    val locationAvailable =
                        withTimeoutOrNull(GPS_WAIT_TIMEOUT_MS) {
                            pinLocationDeferred?.await()
                        }
                    pinLocationDeferred = null
                    when (locationAvailable) {
                        true -> {
                            treeCapturer.setImage(action.imageFile)
                            triggerEvent(
                                NavigationEvent { CaptureFlowScopeManager.nav.navForward(this) },
                            )
                        }
                        false -> {
                            Timber.w("GPS location unavailable, showing bad GPS dialog")
                            updateState { copy(isLocationAvailable = false) }
                        }
                        null -> {
                            Timber.w("Timed out waiting for GPS location")
                            updateState { copy(isLocationAvailable = false) }
                        }
                    }
                }
            }
            is TreeCaptureAction.UpdateBadGpsDialogState -> {
                updateState { copy(isLocationAvailable = action.state) }
            }
            is TreeCaptureAction.UpdateCaptureTutorialDialog -> {
                updateState { copy(showCaptureTutorial = action.show) }
            }
            is TreeCaptureAction.CreateFakeTrees -> {
                viewModelScope.launch {
                    updateState { copy(isCreatingFakeTrees = true) }
                    createFakeTreesUseCase.execute(CreateFakeTreesParams(50))
                    updateState { copy(isCreatingFakeTrees = false) }
                }
            }
        }
    }

    private suspend fun isFirstTrack(): Boolean = userRepo.getPowerUser()!!.numberOfTrees < 1

    companion object {
        private const val GPS_WAIT_TIMEOUT_MS = 30_000L
    }
}

class TreeCaptureViewModelFactory(
    private val profilePicUrl: String,
) : ViewModelProvider.Factory,
    KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        CaptureFlowScopeManager.open()
        return TreeCaptureViewModel(profilePicUrl, get(), get(), get(), get()) as T
    }
}