package org.greenstand.android.TreeTracker.walletselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User

data class WalletSelectState(
    val currentUser: User? = null,
    val alternateUsers: List<User>? = null,
    val selectedUser: User? = null,
)

class WalletSelectViewModel(
    private val users: Users,
) : ViewModel() {

    private val _state = MutableLiveData<WalletSelectState>()
    val state: LiveData<WalletSelectState> = _state

    fun loadPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            val currentPlanter = users.getUser(planterInfoId)
            val alternatePlanters = users.getUsers()
                .filter { it.id != currentPlanter?.id }

            _state.value = WalletSelectState(
                currentUser = currentPlanter,
                alternateUsers = alternatePlanters
            )
        }
    }

    fun selectPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(
                selectedPlanter = users.getUsers().find { planterInfoId == it.id }
            )
        }
    }
}
