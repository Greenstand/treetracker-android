package org.greenstand.android.TreeTracker.userselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Collections.emptyList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
)

class UserSelectViewModel(users: Users) : ViewModel() {

    private val _state = MutableLiveData<UserSelectState>()
    val state: LiveData<UserSelectState> = _state

    init {
        users.users()
            .onEach { userList ->
                _state.value = UserSelectState(users = userList)
            }
            .launchIn(viewModelScope)
    }

    fun selectUser(user: User) {
        _state.value = _state.value?.copy(
            selectedUser = user
        )
    }
}
