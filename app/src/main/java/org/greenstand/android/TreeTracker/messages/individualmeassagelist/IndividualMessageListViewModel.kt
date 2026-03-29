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
package org.greenstand.android.TreeTracker.messages.individualmeassagelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.Message
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Collections

data class IndividualMessageListState(
    val messages: List<Message> = Collections.emptyList(),
    val selectedMessage: Message? = null,
    val currentUser: User? = null,
)

sealed class IndividualMessageListAction : Action {
    data class SelectMessage(val message: Message) : IndividualMessageListAction()
    object NavigateBack : IndividualMessageListAction()
    object NavigateToSelected : IndividualMessageListAction()
}

class IndividualMessageListViewModel(
    private val userId: Long,
    private val userRepo: UserRepo,
    private val messagesRepo: MessagesRepo,
) : BaseViewModel<IndividualMessageListState, IndividualMessageListAction>(IndividualMessageListState()) {

    init {
        viewModelScope.launch {
            val currentUser = userRepo.getUser(userId)
            messagesRepo.getMessageFlow(currentUser!!.wallet)
                .collect { updateMessages(it, currentUser) }
        }
    }

    override fun handleAction(action: IndividualMessageListAction) {
        when (action) {
            is IndividualMessageListAction.SelectMessage -> {
                updateState { copy(selectedMessage = action.message) }
            }
            else -> { }
        }
    }

    private fun updateMessages(messages: List<Message>, currentUser: User) {
        val externalChatMessages = messages
            .filterIsInstance<DirectMessage>()
            .filter { it.from != currentUser.wallet }
            .sortedByDescending { it.composedAt }
            .groupBy { it.from }

        val chatsToShow = externalChatMessages.keys
            .map { externalChatMessages[it]!!.first() }
        val surveysToShow = messages
            .filterIsInstance<SurveyMessage>()
            .filterNot { it.isComplete }
        val announcementsToShow = messages.filterIsInstance<AnnouncementMessage>()

        val messagesToShow = (chatsToShow + surveysToShow + announcementsToShow)
            .sortedByDescending { it.composedAt }

        updateState {
            copy(
                currentUser = currentUser,
                messages = messagesToShow,
            )
        }
    }
}

class IndividualMessageListViewModelFactory(private val userId: Long) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IndividualMessageListViewModel(userId, get(), get()) as T
    }
}
