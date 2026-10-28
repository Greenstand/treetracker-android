/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Collections

data class ChatState(
    val from: String = "",
    val messages: List<DirectMessage> = Collections.emptyList(),
    val draftText: String = "",
    val currentUser: User? = null,
)

sealed class ChatAction : Action {
    data class UpdateDraftText(
        val text: String,
    ) : ChatAction()

    object SendMessage : ChatAction()

    object NavigateBack : ChatAction()
}

class ChatViewModel(
    private val userId: Long,
    private val otherChatIdentifier: String,
    private val userRepo: UserRepo,
    private val messagesRepo: MessagesRepo,
) : BaseViewModel<ChatState, ChatAction>(ChatState()) {
    init {
        viewModelScope.launch {
            val currentUser = userRepo.getUser(userId)
            messagesRepo.getDirectMessages(currentUser!!.wallet, otherChatIdentifier).collect { messages ->
                updateState {
                    copy(
                        from = otherChatIdentifier,
                        currentUser = currentUser,
                        messages = messages,
                    )
                }
                val unreadMessages = messages.filterNot { it.isRead }.map { it.id }
                messagesRepo.markMessagesAsRead(unreadMessages)
            }
        }
    }

    override fun handleAction(action: ChatAction) {
        when (action) {
            is ChatAction.UpdateDraftText -> {
                updateState { copy(draftText = action.text) }
            }
            is ChatAction.SendMessage -> {
                viewModelScope.launch {
                    messagesRepo.saveMessage(
                        currentState.currentUser!!.wallet,
                        otherChatIdentifier,
                        currentState.draftText,
                    )
                    updateState { copy(draftText = "") }
                }
            }
            else -> { }
        }
    }

    fun checkChatAuthor(
        index: Int,
        isFirstMessage: Boolean,
    ): Boolean {
        val messages = currentState.messages
        val prevAuthor = messages.getOrNull(index - 1)?.from
        val nextAuthor = messages.getOrNull(index + 1)?.from
        val content = messages[index]
        val isFirstMessageByAuthor = prevAuthor != content.from
        val isLastMessageByAuthor = nextAuthor != content.from
        return if (isFirstMessage) isFirstMessageByAuthor else isLastMessageByAuthor
    }

    fun checkIsOtherUser(index: Int): Boolean = currentState.messages[index].from == otherChatIdentifier
}

class ChatViewModelFactory(
    private val userId: Long,
    private val otherChatIdentifier: String,
) : ViewModelProvider.Factory,
    KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(userId, otherChatIdentifier, get(), get()) as T
}