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
package org.greenstand.android.TreeTracker.userselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import java.util.Collections.emptyList

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
)

class UserSelectViewModel(
    userRepo: UserRepo,
    locationDataCapturer: LocationDataCapturer,
) : ViewModel() {

    private val _state = MutableStateFlow(UserSelectState())
    val state: Flow<UserSelectState> = _state

    init {
        CaptureSetupScopeManager.open()
        locationDataCapturer.startGpsUpdates()
        userRepo.users()
            .onEach { userList ->
                _state.value = UserSelectState(users = userList)
            }
            .launchIn(viewModelScope)
    }

    fun selectUser(user: User) {
        CaptureSetupScopeManager.getData().user = user
        _state.value = _state.value.copy(
            selectedUser = user
        )
    }
}
