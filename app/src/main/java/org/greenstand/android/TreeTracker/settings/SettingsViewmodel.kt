package org.greenstand.android.TreeTracker.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class SettingsState(
    val showPrivacyPolicyDialog: Boolean? = null,
    val showLogoutDialog: Boolean? = null,
    val showDeleteAccountDialog: Boolean? = null,
    val powerUser: User? = null
)

sealed class SettingsAction : Action {
    data class SetPrivacyDialogVisibility(val show: Boolean) : SettingsAction()
    object Logout : SettingsAction()
    data class UpdateLogoutDialogVisibility(val show: Boolean) : SettingsAction()
    object NavigateToProfile : SettingsAction()
    object NavigateToMap : SettingsAction()
    object NavigateToDeleteAccount : SettingsAction()
    object NavigateBack : SettingsAction()
    object LogoutConfirmed : SettingsAction()
}

class SettingsViewModel(
    private val userRepo: UserRepo,
) : BaseViewModel<SettingsState, SettingsAction>(SettingsState()) {

    init {
        viewModelScope.launch {
            val powerUser = userRepo.getPowerUser()
            updateState { copy(powerUser = powerUser) }
        }
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetPrivacyDialogVisibility -> {
                updateState { copy(showPrivacyPolicyDialog = action.show) }
            }
            is SettingsAction.Logout -> {
                viewModelScope.launch {
                    currentState.powerUser?.let {
                        userRepo.setPowerUserStatus(it.id, false)
                    }
                    handleAction(SettingsAction.UpdateLogoutDialogVisibility(false))
                }
            }
            is SettingsAction.UpdateLogoutDialogVisibility -> {
                updateState { copy(showLogoutDialog = action.show) }
            }
            else -> { }
        }
    }
}
