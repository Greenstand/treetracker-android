package org.greenstand.android.TreeTracker.messages.announcementmessage


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreenState
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
    val currentBody: String? = null,
    val currentUrl: String? = null,
    val currentTitle: String? = null,
)

class AnnouncementViewModel(
    private val messageId: String,
    private val messagesRepo: MessagesRepo,
) : ViewModel() {
    private var _state: MutableStateFlow<AnnouncementState> = MutableStateFlow(AnnouncementState())
    val state: StateFlow<AnnouncementState> = _state.asStateFlow()

    private lateinit var announcement: AnnouncementMessage

    init {
        viewModelScope.launch {
            announcement = messagesRepo.getAnnouncementMessages(messageId)
            _state.value = _state.value.copy(
                currentTitle = announcement.subject,
                currentBody = announcement.body,
                currentUrl = announcement.videoLink
            )
            messagesRepo.markMessageAsRead(messageId)
        }
    }
}


class AnnouncementViewModelFactory(
    private val messageId: String,
) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AnnouncementViewModel(messageId, get()) as T
    }
}