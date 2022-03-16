package org.greenstand.android.TreeTracker.messages


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.*


data class ChatState(
    val messages: Flow<List<DirectMessage>>? = null,
    val draftText: String = "",
    val currentUser: User? = null,
)

class ChatViewModel(
    private val userId: Long,
    private val otherChatIdentifier: String,
    private val users: Users,
    private val messagesRepo: MessagesRepo,
) : ViewModel() {

    private val _state = MutableLiveData<ChatState>()
    val state: LiveData<ChatState> = _state

    init {
        viewModelScope.launch {
            val currentUser = users.getUser(userId)
            val messages = messagesRepo.getDirectMessages(currentUser!!.wallet, otherChatIdentifier)
            _state.value = ChatState(
                currentUser = currentUser,
                messages = messages,
            )
        }
    }

    fun updateDraftText(text: String) {
        _state.value = _state.value!!.copy(
            draftText = text
        )
    }

    fun sendMessage() {
        viewModelScope.launch {
            messagesRepo.saveMessage(
                _state.value!!.currentUser!!.wallet,
                otherChatIdentifier,
                _state.value!!.draftText
            )
            _state.value = _state.value!!.copy(
                draftText = ""
            )
        }
    }
}

class ChatViewModelFactory(private val userId: Long, private val otherChatIdentifier: String) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChatViewModel(userId, otherChatIdentifier, get(), get()) as T
    }
}



