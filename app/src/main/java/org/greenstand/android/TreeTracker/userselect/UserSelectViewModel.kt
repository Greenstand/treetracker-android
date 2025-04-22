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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Collections.emptyList

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val editMode: Boolean = false,
)

class UserSelectViewModel(
    userId: Long?,
    private val userRepo: UserRepo,
    locationDataCapturer: LocationDataCapturer,
    private val prefs: Preferences,
) : ViewModel() {

    private val _state = MutableStateFlow(UserSelectState())
    val state: Flow<UserSelectState> = _state

    init {
        CaptureSetupScopeManager.open()
        locationDataCapturer.startGpsUpdates()
        userRepo.users()
            .onEach { userList ->
                _state.value = _state.value.copy(users = userList)
            }
            .launchIn(viewModelScope)
        if(userId != null){
            viewModelScope.launch(Dispatchers.IO) {
                val user= userRepo.getUser(userId)
                _state.value = _state.value.copy(
                    selectedUser = user
                )
            }

        }
    }

    fun selectUser(user: User) {
        prefs.setUserId(user.id)
        CaptureSetupScopeManager.getData().user = user
        _state.value = _state.value.copy(
            selectedUser = user
        )
    }

    // 🔁 Toggle edit mode
    fun updateEditEnabled() {
        _state.value = _state.value.copy(editMode = !_state.value.editMode)
    }

    // 🧠 Update only specified fields in selectedUser
    fun updateSelectedUser(
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        email: String? = null,
        photoPath: String? = null
    ) {
        val currentUser = _state.value.selectedUser ?: return
        val updatedUser = currentUser.copy(
            firstName = firstName ?: currentUser.firstName,
            lastName = lastName ?: currentUser.lastName,
            wallet = phone ?: email ?: currentUser.wallet,
            photoPath = photoPath ?: currentUser.photoPath,
        )
        _state.value = _state.value.copy(selectedUser = updatedUser)
    }

    //  Save current selectedUser to database
    fun updateUserInDatabase() {
         val user = _state.value.selectedUser ?: return

        viewModelScope.launch(Dispatchers.IO) {

            userRepo.updateUser(user)
        }
    }
}

class UserSelectViewModelFactory(private val userId: Long) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserSelectViewModel(userId, get(), get(),get(),) as T
    }
}