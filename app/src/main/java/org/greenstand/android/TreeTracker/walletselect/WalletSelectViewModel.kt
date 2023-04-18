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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User

data class WalletSelectState(
    val currentUser: User? = null,
    val alternateUsers: List<User> = emptyList(),
    val selectedUser: User? = null,
)

class WalletSelectViewModel(private val userRepo: UserRepo) : ViewModel() {

    private val _state = MutableStateFlow(WalletSelectState())
    val state: Flow<WalletSelectState> = _state


    init {
        viewModelScope.launch {
            val currentUser = CaptureSetupScopeManager.getData().user
            userRepo.users()
                .map { users -> users.filter { it.id != currentUser?.id } }
                .onEach { users ->
                    _state.value = _state.value.copy(
                        currentUser = currentUser,
                        alternateUsers = users
                    )
                }
                .launchIn(this)
        }
    }

    fun selectPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            val selectedUser = userRepo.getUserList().find { planterInfoId == it.id }
            CaptureSetupScopeManager.getData().destinationWallet = selectedUser?.wallet
            _state.value = _state.value.copy(
                selectedUser = selectedUser
            )
        }
    }
}
