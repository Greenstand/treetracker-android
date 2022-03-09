package org.greenstand.android.TreeTracker.models.messages.network

import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MessagesApiService {

    @GET("messaging/message")
    suspend fun getMessages(@Query("handle") wallet: String): MessagesResponse

}