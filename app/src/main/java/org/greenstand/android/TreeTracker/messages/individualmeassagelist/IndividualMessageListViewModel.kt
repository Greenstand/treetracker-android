package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User

data class IndividualMessageListState(
    val currentUser: User? = null,
)

class IndividualMessageListViewModel(
    private val users: Users,
) : ViewModel() {
    private val _state = MutableLiveData<IndividualMessageListState>()
    val state: LiveData<IndividualMessageListState> = _state


    fun loadPlanter(planterInfoId: Long) {
        viewModelScope.launch {
            val currentUser = users.getUser(planterInfoId)
            _state.value = IndividualMessageListState(currentUser = currentUser)
        }
    }



}