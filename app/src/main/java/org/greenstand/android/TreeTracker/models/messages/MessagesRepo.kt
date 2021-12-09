package org.greenstand.android.TreeTracker.models.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService

class MessagesRepo(private val apiService: MessagesApiService) {

    suspend fun getMessages(wallet: String): List<Message> = withContext(Dispatchers.IO) {
        val result = apiService.getMessages(wallet)
        return@withContext result.messages.map { it.toMessage() }
    }
}