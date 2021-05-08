package org.greenstand.android.TreeTracker.userselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User
import java.util.Collections.emptyList

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
)

class UserSelectViewModel(private val users: Users) : ViewModel() {

    private val _state = MutableLiveData<UserSelectState>()
    val state: LiveData<UserSelectState> = _state

    init {
        viewModelScope.launch {
            _state.value = UserSelectState(
                users = users.getUsers(),
            )
        }
    }

    fun selectUser(user: User) {
        _state.value = _state.value?.copy(
            selectedUser = user
        )
    }
}
