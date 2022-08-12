package org.greenstand.android.TreeTracker.walletselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

    private val _state = MutableLiveData<WalletSelectState>()
    val state: LiveData<WalletSelectState> = _state

    init {
        viewModelScope.launch {
            val currentUser = CaptureSetupScopeManager.getData().user
            userRepo.users()
                .map { users -> users.filter { it.id != currentUser?.id } }
                .onEach { users ->
                    _state.value = _state.value?.copy(
                        currentUser = currentUser,
                        alternateUsers = users
                    ) ?: WalletSelectState(
                        currentUser = currentUser,
                        alternateUsers = users
                    )}
                .launchIn(this)
        }
    }

    fun selectPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            val selectedUser = userRepo.getUserList().find { planterInfoId == it.id }
            CaptureSetupScopeManager.getData().destinationWallet = selectedUser?.wallet
            _state.value = _state.value?.copy(
                selectedUser = selectedUser
            )
        }
    }
}
