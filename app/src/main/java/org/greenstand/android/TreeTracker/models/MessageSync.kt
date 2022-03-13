package org.greenstand.android.TreeTracker.models

import org.greenstand.android.TreeTracker.models.messages.MessagesRepo

class MessageSync(
    private val messagesRepo: MessagesRepo,
) {

    suspend fun fetchMessages() {
        messagesRepo.fetchMessages()
    }

    suspend fun uploadMessages() {

    }
}