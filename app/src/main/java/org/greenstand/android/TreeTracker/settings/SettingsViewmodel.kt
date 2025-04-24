package org.greenstand.android.TreeTracker.settings


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User

data class SettingsState(
    val showPrivacyPolicyDialog: Boolean? = null,
    val showLogoutDialog: Boolean? = null,
    val showDeleteAccountDialog: Boolean? = null,
    val powerUser: User? = null
)

class SettingsViewModel(
    private val userRepo: UserRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: Flow<SettingsState> = _state

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                powerUser = userRepo.getPowerUser()
            )
        }
    }

    fun setPrivacyDialogVisibility(show: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                showPrivacyPolicyDialog = show
            )
        }
    }
    fun logout(){
        viewModelScope.launch {
            _state.value.powerUser?.let {
                userRepo.setPowerUserStatus(it.id, false)
            }
            updateLogoutDialogVisibility(false)
        }
    }
    fun updateLogoutDialogVisibility(show: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                showLogoutDialog = show
            )
        }
    }
}
