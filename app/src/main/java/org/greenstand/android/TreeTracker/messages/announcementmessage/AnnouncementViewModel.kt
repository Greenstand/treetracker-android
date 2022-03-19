package org.greenstand.android.TreeTracker.messages.announcementmessage


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.*


data class AnnouncementState(
    val messages: List<AnnouncementMessage> = Collections.emptyList(),
    val currentUser: User? = null,
    val currentBody: String? = null,
    val currentUrl: String? = null,
)

class AnnouncementViewModel(
    private val userId: Long,
    private val otherChatIdentifier: String,
    private val users: Users,
    private val messagesRepo: MessagesRepo,
) : ViewModel() {
    private val _state = MutableLiveData<AnnouncementState>()
    val state: LiveData<AnnouncementState> = _state
    private lateinit var announcement: List<AnnouncementMessage>
    private var currentAnnouncementIndex: Int = 0

    init {
        viewModelScope.launch {
            val currentUser = users.getUser(userId)
            messagesRepo.getAnnouncementMessages(currentUser!!.wallet, otherChatIdentifier)
                .collect { messages ->
                    _state.value = AnnouncementState(
                        currentUser = currentUser,
                        messages = messages,
                        currentBody = messages[currentAnnouncementIndex].body,
                        currentUrl = messages[currentAnnouncementIndex].videoLink
                    )
                    announcement = messages
                    val unreadMessages = messages.filterNot { it.isRead }.map { it.id }
                    messagesRepo.markMessagesAsRead(unreadMessages)
                }
        }
    }

    fun goToNextAnnouncement(): Boolean {
        if (currentAnnouncementIndex == _state.value?.messages!!.size - 1) {
            return false
        }
        currentAnnouncementIndex++
        _state.value = _state.value!!.copy(
            currentBody = announcement[currentAnnouncementIndex].body,
            currentUrl = announcement[currentAnnouncementIndex].videoLink
        )
        return true
    }

    fun goToPrevAnnouncement(): Boolean {
        if (currentAnnouncementIndex == 0) {
            return false
        }
        currentAnnouncementIndex--
        _state.value = _state.value?.copy(
            currentBody = announcement[currentAnnouncementIndex].body,
            currentUrl = announcement[currentAnnouncementIndex].videoLink
        )
        return true
    }

}

class AnnouncementViewModelFactory(
    private val userId: Long,
    private val otherChatIdentifier: String
) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AnnouncementViewModel(userId, otherChatIdentifier, get(), get()) as T
    }
}