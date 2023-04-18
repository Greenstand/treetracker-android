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
package org.greenstand.android.TreeTracker.models.messages.network

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MessagesApiService {

    @GET("messaging/message")
    suspend fun getMessages(
        @Header("apiKey") apiKey: String = BuildConfig.TREETRACKER_CLIENT_SECRET,
        @Query("handle") wallet: String,
        @Query("since") lastSyncTime: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = 10,
    ): MessagesResponse

}