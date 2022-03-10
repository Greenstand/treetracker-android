package org.greenstand.android.TreeTracker.models.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService

/**
 * For test data, use 'handle3' as a wallet
 */
class MessagesRepo(private val apiService: MessagesApiService) {

    suspend fun getMessages(wallet: String): List<Message> = withContext(Dispatchers.IO) {
        val result = apiService.getMessages(wallet)
        return@withContext result.messages.map { it.toMessage() }
    }

    suspend fun getDirectMessages(
        wallet: String,
        from: String,
        to: String): List<DirectMessage> = withContext(Dispatchers.IO) {
        return@withContext getMessages(wallet)
            .filterIsInstance<DirectMessage>()
            .filter { (it.from == from || it.from == to) && (it.to == to || it.to == from) }
            .sortedBy { it.composedAt }
    }
}