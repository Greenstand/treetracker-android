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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Collections.emptyList

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val editMode: Boolean = false,
    val deleteProfileState: DeleteProfileState = DeleteProfileState.INITIAL
)
enum class DeleteProfileState(){
    INITIAL,
    SHOWDIALOG,
    DISMISSDIALOG,
    ACCOUNTDELETEDLOCALLY,
    ACCOUNTDELETEDANDADMINREQUESTED
}

sealed class UserSelectAction : Action {
    data class SelectUser(val user: User) : UserSelectAction()
    object DeleteUser : UserSelectAction()
    object ToggleEditMode : UserSelectAction()
    data class UpdateSelectedUser(
        val firstName: String? = null,
        val lastName: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val photoPath: String? = null
    ) : UserSelectAction()
    object SaveUserToDatabase : UserSelectAction()
    data class UpdateDeleteProfileState(val deleteProfileState: DeleteProfileState) : UserSelectAction()
    object NavigateBack : UserSelectAction()
    object NavigateToPhoto : UserSelectAction()
}

class UserSelectViewModel(
    userId: Long?,
    private val userRepo: UserRepo,
    private val messageRepo: MessagesRepo,
    locationDataCapturer: LocationDataCapturer,
    private val prefs: Preferences,
) : BaseViewModel<UserSelectState, UserSelectAction>(UserSelectState()) {

    init {
        CaptureSetupScopeManager.open()
        locationDataCapturer.startGpsUpdates()
        userRepo.users()
            .onEach { userList ->
                updateState { copy(users = userList) }
            }
            .launchIn(viewModelScope)
        if(userId != null){
            viewModelScope.launch(Dispatchers.IO) {
                val user = userRepo.getUser(userId)
                updateState { copy(selectedUser = user) }
            }
        }
    }

    override fun handleAction(action: UserSelectAction) {
        when (action) {
            is UserSelectAction.SelectUser -> {
                prefs.setUserId(action.user.id)
                CaptureSetupScopeManager.getData().user = action.user
                updateState { copy(selectedUser = action.user) }
            }
            is UserSelectAction.DeleteUser -> {
                viewModelScope.launch {
                    if(userRepo.deleteUser(currentState.selectedUser?.wallet ?: "")) {
                        updateState { copy(deleteProfileState = DeleteProfileState.ACCOUNTDELETEDLOCALLY) }
                        messageRepo.saveMessage(
                            wallet = currentState.selectedUser?.wallet ?: "",
                            to = "admin",
                            body = "Hi admin, I would like to delete my account with the wallet ${currentState.selectedUser?.wallet} I understand that all my data will be lost."
                        )
                        messageRepo.syncMessages()
                    }
                }
            }
            is UserSelectAction.ToggleEditMode -> {
                updateState { copy(editMode = !editMode) }
            }
            is UserSelectAction.UpdateSelectedUser -> {
                val currentUser = currentState.selectedUser ?: return
                val updatedUser = currentUser.copy(
                    firstName = action.firstName ?: currentUser.firstName,
                    lastName = action.lastName ?: currentUser.lastName,
                    wallet = action.phone ?: action.email ?: currentUser.wallet,
                    photoPath = action.photoPath ?: currentUser.photoPath,
                )
                updateState { copy(selectedUser = updatedUser) }
            }
            is UserSelectAction.SaveUserToDatabase -> {
                val user = currentState.selectedUser ?: return
                viewModelScope.launch(Dispatchers.IO) {
                    userRepo.updateUser(user)
                }
            }
            is UserSelectAction.UpdateDeleteProfileState -> {
                updateState { copy(deleteProfileState = action.deleteProfileState) }
            }
            else -> { }
        }
    }
}

class UserSelectViewModelFactory(private val userId: Long) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserSelectViewModel(userId, get(), get(), get(), get()) as T
    }
}
