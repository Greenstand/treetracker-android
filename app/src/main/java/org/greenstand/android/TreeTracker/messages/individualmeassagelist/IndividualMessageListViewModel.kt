package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.Message
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import org.greenstand.android.TreeTracker.models.user.User
import java.util.*

data class IndividualMessageListState(
    val messages: List<Message> = Collections.emptyList(),
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