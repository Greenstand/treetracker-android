package org.greenstand.android.TreeTracker.walletselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User

data class WalletSelectState(
    val currentUser: User? = null,
    val alternateUsers: List<User> = emptyList(),
    val selectedUser: User? = null,
)

class WalletSelectViewModel(
    private val users: Users,
    private val stepCounter: StepCounter,
) : ViewModel() {

    private val _state = MutableLiveData<WalletSelectState>()
    val state: LiveData<WalletSelectState> = _state

    fun loadPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            val currentUser = users.getUser(planterInfoId)
            users.users()
                .map { users -> users.filter { it.id != currentUser?.id } }
                .onEach { users ->
                    _state.value = WalletSelectState(
                        currentUser = currentUser,
                        alternateUsers = users
                    )}
                .launchIn(this)
        }
    }

    fun selectPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(
                selectedUser = users.getUserList().find { planterInfoId == it.id }
            )
        }
    }

    fun startSession(user: User) {
        viewModelScope.launch {
            stepCounter.enable()
            users.startUserSession("", user.id)
        }
    }
}
