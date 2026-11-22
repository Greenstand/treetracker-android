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
    private val setupRestorer: CaptureSetupRestorer,
) : BaseViewModel<WalletSelectState, WalletSelectAction>(WalletSelectState()) {
    init {
        // After process death Nav3 restores wallet select, but the capture setup scope only lived
        // in memory. Reopen it from the copy on disk before reading it (#1447). No wallet is
        // selected on the restored screen, so the user picks one again before Next.
        val restoring = setupRestorer.reopenIfClosed()
        if (restoring) updateState { copy(restoring = true) }
        viewModelScope.launch {
            if (restoring && !setupRestorer.reloadUser()) {
                updateState { copy(restoring = false, setupLost = true) }
                return@launch
            }
            setupRestorer.save()
            val currentUser = CaptureSetupScopeManager.getData().user
            userRepo
                .users()
                .map { users -> users.filter { it.id != currentUser?.id } }
                .onEach { users ->
                    updateState {
                        copy(restoring = false, currentUser = currentUser, alternateUsers = users)
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
                    // Setup can be lost while the user list loads.
                    if (currentState.setupLost) return@launch
                    CaptureSetupScopeManager.getData().destinationWallet = selectedUser?.wallet
                    setupRestorer.save()
                    updateState { copy(selectedUser = selectedUser) }
                }
            }
            else -> { }
        }
    }
}