package org.greenstand.android.TreeTracker.userselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.Users

data class UserSelectState(
    val planters: List<PlanterInfoEntity> = emptyList(),
    val selectedPlanter: PlanterInfoEntity? = null,
)

class UserSelectViewModel(private val users: Users) : ViewModel() {

    private val _state = MutableLiveData<UserSelectState>()
    val state: LiveData<UserSelectState> = _state

    init {
        viewModelScope.launch {
            _state.value = UserSelectState(
                planters = users.getUsers(),
            )
        }
    }

    fun selectUser(planterInfo: PlanterInfoEntity) {
        _state.value = _state.value?.copy(
            selectedPlanter = planterInfo
        )
    }

}