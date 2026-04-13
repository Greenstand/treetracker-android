/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class SettingsState(
    val showPrivacyPolicyDialog: Boolean? = null,
    val showLogoutDialog: Boolean? = null,
    val showDeleteAccountDialog: Boolean? = null,
    val powerUser: User? = null,
)

sealed class SettingsAction : Action {
    data class SetPrivacyDialogVisibility(
        val show: Boolean,
    ) : SettingsAction()

    object Logout : SettingsAction()

    data class UpdateLogoutDialogVisibility(
        val show: Boolean,
    ) : SettingsAction()

    object NavigateToProfile : SettingsAction()

    object NavigateToMap : SettingsAction()

    object NavigateToDeleteAccount : SettingsAction()

    object NavigateToEditTrees : SettingsAction()

    object NavigateBack : SettingsAction()

    object LogoutConfirmed : SettingsAction()
}

class SettingsViewModel(
    private val userRepo: UserRepo,
) : BaseViewModel<SettingsState, SettingsAction>(SettingsState()) {
    init {
        viewModelScope.launch {
            val powerUser = userRepo.getPowerUser()
            updateState { copy(powerUser = powerUser) }
        }
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetPrivacyDialogVisibility -> {
                updateState { copy(showPrivacyPolicyDialog = action.show) }
            }
            is SettingsAction.Logout -> {
                viewModelScope.launch {
                    currentState.powerUser?.let {
                        userRepo.setPowerUserStatus(it.id, false)
                    }
                    handleAction(SettingsAction.UpdateLogoutDialogVisibility(false))
                }
            }
            is SettingsAction.UpdateLogoutDialogVisibility -> {
                updateState { copy(showLogoutDialog = action.show) }
            }
            else -> { }
        }
    }
}