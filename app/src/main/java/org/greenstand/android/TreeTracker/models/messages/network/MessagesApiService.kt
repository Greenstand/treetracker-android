package org.greenstand.android.TreeTracker.models.messages.network

import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import retrofit2.http.GET

interface MessagesApiService {

    @GET("/messages")
    suspend fun getMessages(): MessageResponse

}