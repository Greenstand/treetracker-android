package org.greenstand.android.TreeTracker.messages


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.*


data class ChatState(
    val from: String = "",
    val messages: List<DirectMessage> = Collections.emptyList(),
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
            messagesRepo.getDirectMessages(currentUser!!.wallet, otherChatIdentifier).collect { messages ->
                _state.value = ChatState(
                    from = otherChatIdentifier,
                    currentUser = currentUser,
                    messages = messages,
                )
                val unreadMessages = messages.filterNot { it.isRead }.map { it.id }
                messagesRepo.markMessagesAsRead(unreadMessages)
            }
        }
    }

    fun updateDraftText(text: String) {
        _state.value = _state.value!!.copy(
            draftText = text
        )
    }

    fun checkChatAuthor(index :Int,isFirstMessage: Boolean ):Boolean{
        val messages = _state.value!!.messages
        val prevAuthor = messages.getOrNull(index - 1)?.from
        val nextAuthor = messages.getOrNull(index + 1)?.from
        val content = messages[index]
        val isFirstMessageByAuthor = prevAuthor != content.from
        val isLastMessageByAuthor = nextAuthor != content.from
        return if(isFirstMessage) isFirstMessageByAuthor else isLastMessageByAuthor
    }

    fun checkIsOtherUser(index :Int): Boolean{
        return _state.value!!.messages[index].from == otherChatIdentifier
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



