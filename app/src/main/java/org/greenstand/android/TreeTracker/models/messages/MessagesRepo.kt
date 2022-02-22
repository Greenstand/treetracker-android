package org.greenstand.android.TreeTracker.models.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService

class MessagesRepo(private val apiService: MessagesApiService) {

    suspend fun getMessages(wallet: String): List<Message> = withContext(Dispatchers.IO) {
        val result = apiService.getMessages(wallet)
        return@withContext result.messages.map { it.toMessage() }
    }

    suspend fun getTextMessages(
        wallet: String,
        from: String,
        to: String): List<TextMessage> = withContext(Dispatchers.IO) {
        return@withContext getMessages(wallet)
            .filterIsInstance<TextMessage>()
            .filter { (it.from == from || it.from == to) && (it.to == to || it.to == from) }
            .sortedBy { it.composedAt }
    }
}