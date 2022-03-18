package org.greenstand.android.TreeTracker.messages.announcementmessage


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.*


data class AnnouncementState(
    val messages: List<AnnouncementMessage> = Collections.emptyList(),
    val currentUser: User? = null,
    val isInternetAvailable: Boolean = false,
    val showNoInternetDialog: Boolean = false,
    )

class AnnouncementViewModel(
    private val userId: Long,
    private val otherChatIdentifier: String,
    private val users: Users,
    private val messagesRepo: MessagesRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase,
) : ViewModel() {
    private val _state = MutableLiveData<AnnouncementState>()
    val state: LiveData<AnnouncementState> = _state

    init {
        viewModelScope.launch {
            val currentUser = users.getUser(userId)
            val messages =
                messagesRepo.getAnnouncementMessages(currentUser!!.wallet, otherChatIdentifier).collect {
                    _state.value =AnnouncementState(
                        currentUser = currentUser,
                        messages = it,
                    )
                }
            val result = checkForInternetUseCase.execute(Unit)
            _state.value = _state.value?.copy(isInternetAvailable = result,)

        }
    }

    fun updateNoInternetDialogState(state: Boolean) {
        _state.value = _state.value?.copy(showNoInternetDialog = state,)
    }

}

class AnnouncementViewModelFactory(private val userId: Long, private val otherChatIdentifier: String) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AnnouncementViewModel(userId, otherChatIdentifier, get(), get(),get()) as T
    }
}