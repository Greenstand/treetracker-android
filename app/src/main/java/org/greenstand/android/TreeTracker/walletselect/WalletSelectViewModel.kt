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
package org.greenstand.android.TreeTracker.walletselect

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class WalletSelectState(
    val currentUser: User? = null,
    val alternateUsers: List<User> = emptyList(),
    val selectedUser: User? = null,
    /** True while the screen, restored after process death, reloads its setup user. Next and Back wait. */
    val restoring: Boolean = false,
    /** True when the screen came back after process death and its user could not be found again. */
    val setupLost: Boolean = false,
)

sealed class WalletSelectAction : Action {
    data class SelectPlanter(
        val planterInfoId: Long,
    ) : WalletSelectAction()

    object NavigateToUserSelect : WalletSelectAction()

    object NavigateForward : WalletSelectAction()

    object NavigateToAddWallet : WalletSelectAction()

    object NavigateBack : WalletSelectAction()
}

class WalletSelectViewModel(
    private val userRepo: UserRepo,
    private val restorer: CaptureSetupRestorer,
) : BaseViewModel<WalletSelectState, WalletSelectAction>(WalletSelectState()) {
    init {
        // After process death Nav3 restores this screen with the capture setup scope closed (#1447).
        val restored = restorer.reopenScopeIfClosed()
        if (restored) updateState { copy(restoring = true) }
        viewModelScope.launch {
            val currentUser = restorer.setupUser(restored)
            if (restored && currentUser == null) {
                updateState { copy(restoring = false, setupLost = true) }
                return@launch
            }
            updateState { copy(restoring = false) }
            userRepo
                .users()
                .map { users -> users.filter { it.id != currentUser?.id } }
                .onEach { users ->
                    updateState {
                        copy(currentUser = currentUser, alternateUsers = users)
                    }
                }.launchIn(this)
        }
    }

    override fun handleAction(action: WalletSelectAction) {
        // The setup scope is closed once setup is lost; the screen is on its way to the Dashboard.
        if (currentState.setupLost) return
        when (action) {
            is WalletSelectAction.SelectPlanter -> {
                viewModelScope.launch {
                    val selectedUser = userRepo.getUserList().find { action.planterInfoId == it.id }
                    CaptureSetupScopeManager.getData().destinationWallet = selectedUser?.wallet
                    updateState { copy(selectedUser = selectedUser) }
                }
            }
            else -> { }
        }
    }
}