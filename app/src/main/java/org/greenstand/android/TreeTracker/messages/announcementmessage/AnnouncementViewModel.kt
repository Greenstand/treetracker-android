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
package org.greenstand.android.TreeTracker.messages.announcementmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class AnnouncementState(
    val from: String = "",
    val currentBody: String? = null,
    val currentUrl: String? = null,
    val currentTitle: String? = null,
)

sealed class AnnouncementAction : Action {
    object NavigateBack : AnnouncementAction()
    data class OpenLink(val url: String) : AnnouncementAction()
}

class AnnouncementViewModel(
    private val messageId: String,
    private val messagesRepo: MessagesRepo,
) : BaseViewModel<AnnouncementState, AnnouncementAction>(AnnouncementState()) {

    private lateinit var announcement: AnnouncementMessage

    init {
        viewModelScope.launch {
            announcement = messagesRepo.getAnnouncementMessages(messageId)
            updateState {
                copy(
                    from = announcement.from,
                    currentTitle = announcement.subject,
                    currentBody = announcement.body,
                    currentUrl = announcement.videoLink
                )
            }
            messagesRepo.markMessageAsRead(messageId)
        }
    }

    override fun handleAction(action: AnnouncementAction) {
        // No user actions for announcement screen
    }
}

class AnnouncementViewModelFactory(
    private val messageId: String,
) :
    ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnnouncementViewModel(messageId, get()) as T
    }
}
