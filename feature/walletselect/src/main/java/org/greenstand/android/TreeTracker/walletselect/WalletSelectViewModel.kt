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
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class WalletSelectState(
    val currentUser: User? = null,
    val alternateUsers: List<User> = emptyList(),
    val selectedUser: User? = null,
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
) : BaseViewModel<WalletSelectState, WalletSelectAction>(WalletSelectState()) {
    init {
        viewModelScope.launch {
            val currentUser = CaptureSetupScopeManager.getData().user
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