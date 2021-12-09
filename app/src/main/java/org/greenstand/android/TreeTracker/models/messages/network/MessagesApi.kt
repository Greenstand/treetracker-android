package org.greenstand.android.TreeTracker.models.messages.network

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class MessagesApi(apiService: MessagesApiService) {

    init {
        GlobalScope.launch {
            val result = apiService.getMessages()
            Timber.d("JONATHAN $result")
        }
    }

}