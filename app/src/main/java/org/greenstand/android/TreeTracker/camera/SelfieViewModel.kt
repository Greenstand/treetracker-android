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
package org.greenstand.android.TreeTracker.camera

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class SelfieState(
    val showSelfieTutorial: Boolean? = null,
)

sealed class SelfieAction : Action {
    data class UpdateSelfieTutorialDialog(
        val show: Boolean,
    ) : SelfieAction()
}

/**
 * State for [SelfieScreen]: whether to show the selfie tutorial. It's shown on the first sign-up
 * on a device (no admin account yet), the same rule SignupViewModel uses.
 */
class SelfieViewModel(
    private val userRepo: UserRepo,
) : BaseViewModel<SelfieState, SelfieAction>(SelfieState()) {
    init {
        viewModelScope.launch(Dispatchers.Main) {
            val initialSetupRequired = userRepo.getPowerUser() == null
            updateState { copy(showSelfieTutorial = initialSetupRequired) }
        }
    }

    override fun handleAction(action: SelfieAction) {
        when (action) {
            is SelfieAction.UpdateSelfieTutorialDialog -> updateState { copy(showSelfieTutorial = action.show) }
        }
    }
}