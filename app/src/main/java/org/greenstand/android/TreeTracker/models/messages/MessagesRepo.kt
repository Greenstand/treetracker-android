package org.greenstand.android.TreeTracker.models.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import timber.log.Timber

class MessagesRepo(private val apiService: MessagesApiService) {

    suspend fun getMessages(wallet: String): List<Message> = withContext(Dispatchers.IO) {
        Timber.d("JONATHAN START API")
        val result = apiService.getMessages(wallet)
        Timber.d("JONATHAN START CONVERT")
        val r = result.messages.map { it.toMessage() }
        Timber.d("JONATHAN START DONE")
        r
    }
}