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
import kotlinx.coroutines.CancellationException
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
import timber.log.Timber
import java.util.Collections.emptyList

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val editMode: Boolean = false,
    val deleteProfileState: DeleteProfileState = DeleteProfileState.INITIAL,
)

enum class DeleteProfileState {
    INITIAL,
    SHOWDIALOG,
    DISMISSDIALOG,
    ACCOUNTDELETEDLOCALLY,
    ACCOUNTDELETEDANDADMINREQUESTED,
}

sealed class UserSelectAction : Action {
    data class SelectUser(
        val user: User,
    ) : UserSelectAction()

    object DeleteUser : UserSelectAction()

    object ToggleEditMode : UserSelectAction()

    data class UpdateSelectedUser(
        val firstName: String? = null,
        val lastName: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val photoPath: String? = null,
    ) : UserSelectAction()

    object SaveUserToDatabase : UserSelectAction()

    data class UpdateDeleteProfileState(
        val deleteProfileState: DeleteProfileState,
    ) : UserSelectAction()

    object NavigateBack : UserSelectAction()

    object NavigateToPhoto : UserSelectAction()
}

class UserSelectViewModel(
    userId: Long?,
    private val userRepo: UserRepo,
    private val messageRepo: MessagesRepo,
    private val locationDataCapturer: LocationDataCapturer,
    private val prefs: Preferences,
) : BaseViewModel<UserSelectState, UserSelectAction>(UserSelectState()) {
    // The user SelectUser last made the current user, so deleting them can clear it again.
    private var currentUserId: Long? = null

    init {
        startSetup()
        userRepo
            .users()
            .onEach { userList ->
                updateState { copy(users = userList) }
            }.launchIn(viewModelScope)
        if (userId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val user = userRepo.getUser(userId)
                updateState { copy(selectedUser = user) }
            }
        }
    }

    /**
     * Starts capture setup again if capture ended it while this ViewModel stayed alive.
     *
     * Leaving capture for user select (`CaptureFlowNavigationController.goToUserSelect`) closes
     * the setup scope and replaces the back stack with `UserSelectRoute` in one step. That
     * route's entry never leaves the back stack, so Navigation 3 keeps its ViewModel and `init`
     * doesn't run again. The capture setup user select screen calls this each time it is shown:
     * it reopens the setup scope and clears the selection, which belonged to the ended session.
     * While the scope is still open (e.g. Back from a later setup screen) it changes nothing.
     */
    fun resumeSetup() {
        if (CaptureSetupScopeManager.isOpen) return
        startSetup()
        updateState { copy(selectedUser = null) }
    }

    private fun startSetup() {
        CaptureSetupScopeManager.open()
        locationDataCapturer.startGpsUpdates()
    }

    override fun handleAction(action: UserSelectAction) {
        when (action) {
            is UserSelectAction.SelectUser -> {
                prefs.setUserId(action.user.id)
                currentUserId = action.user.id
                CaptureSetupScopeManager.getData().user = action.user
                updateState { copy(selectedUser = action.user) }
            }
            is UserSelectAction.DeleteUser -> deleteUser()
            is UserSelectAction.ToggleEditMode -> {
                updateState { copy(editMode = !editMode) }
            }
            is UserSelectAction.UpdateSelectedUser -> updateSelectedUser(action)
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

    private fun deleteUser() {
        viewModelScope.launch {
            val deletedUser = currentState.selectedUser
            if (userRepo.deleteUser(deletedUser?.wallet ?: "")) {
                deletedUser?.let { clearCurrentUser(it) }
                updateState { copy(deleteProfileState = DeleteProfileState.ACCOUNTDELETEDLOCALLY) }
                messageRepo.saveMessage(
                    wallet = currentState.selectedUser?.wallet ?: "",
                    to = "admin",
                    body = "Hi admin, I would like to delete my account with the wallet ${currentState.selectedUser?.wallet} I understand that all my data will be lost.",
                )
                // The user is already deleted locally and the message is queued, so a failed sync
                // (for example offline) must not crash the app; the next sync uploads it.
                try {
                    messageRepo.syncMessages()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Message sync after deleting a user failed")
                }
            }
        }
    }

    /** A deleted user must not stay the current user that SelectUser set. */
    private fun clearCurrentUser(deletedUser: User) {
        // Only clear the id this ViewModel set. Today only DeleteProfileScreen deletes, and it
        // always sends SelectUser first, so this is a defensive check.
        if (currentUserId == deletedUser.id) {
            prefs.setUserId(null)
            currentUserId = null
        }
        // The delete suspended, so the setup scope may have closed since; getData() would throw.
        if (!CaptureSetupScopeManager.isOpen) return
        val setupData = CaptureSetupScopeManager.getData()
        if (setupData.user?.id == deletedUser.id) {
            setupData.user = null
        }
    }

    private fun updateSelectedUser(action: UserSelectAction.UpdateSelectedUser) {
        val currentUser = currentState.selectedUser ?: return
        val updatedUser =
            currentUser.copy(
                firstName = action.firstName ?: currentUser.firstName,
                lastName = action.lastName ?: currentUser.lastName,
                wallet = action.phone ?: action.email ?: currentUser.wallet,
                photoPath = action.photoPath ?: currentUser.photoPath,
            )
        updateState { copy(selectedUser = updatedUser) }
    }
}

class UserSelectViewModelFactory(
    private val userId: Long,
) : ViewModelProvider.Factory,
    KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = UserSelectViewModel(userId, get(), get(), get(), get()) as T
}