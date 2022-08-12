package org.greenstand.android.TreeTracker.userselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import java.util.Collections.emptyList

data class UserSelectState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
)

class UserSelectViewModel(
    userRepo: UserRepo,
    locationDataCapturer: LocationDataCapturer,
) : ViewModel() {

    private val _state = MutableLiveData<UserSelectState>()
    val state: LiveData<UserSelectState> = _state

    init {
        CaptureSetupScopeManager.open()
        locationDataCapturer.startGpsUpdates()
        userRepo.users()
            .onEach { userList ->
                _state.value = UserSelectState(users = userList)
            }
            .launchIn(viewModelScope)
    }

    fun selectUser(user: User) {
        CaptureSetupScopeManager.getData().user = user
        _state.value = _state.value?.copy(
            selectedUser = user
        )
    }
}
