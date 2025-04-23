package org.greenstand.android.TreeTracker.settings


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo

data class SettingsState(
    val showPrivacyPolicyDialog: Boolean? = null,
)

class SettingsViewModel(
    private val userRepo: UserRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: Flow<SettingsState> = _state

    init {

    }

    fun setPrivacyDialogVisibility(show: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                showPrivacyPolicyDialog = show
            )
        }
    }
}
