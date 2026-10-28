/*
 * Copyright 2026 Treetracker
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

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.utilities.Constants
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exercises the Retrofit definition of [MessagesApiService] against canned responses served by an
 * OkHttp interceptor, so no network is used.
 */
class MessagesApiServiceTest {
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Volatile
    private var lastRequest: Request? = null

    @Test
    fun `getMessages calls the messaging endpoint with query parameters and api key header`() =
        runTest {
            val service = createService(code = 200, body = EMPTY_PAGE)

            service.getMessages(
                apiKey = "test-api-key",
                wallet = "planter@test",
                lastSyncTime = "2023-05-01T10:00:00Z",
                offset = 20,
                limit = 100,
            )

            val request = lastRequest!!
            assertEquals("GET", request.method)
            assertEquals("/messaging/message", request.url.encodedPath)
            assertEquals("planter@test", request.url.queryParameter("handle"))
            assertEquals("2023-05-01T10:00:00Z", request.url.queryParameter("since"))
            assertEquals("20", request.url.queryParameter("offset"))
            assertEquals("100", request.url.queryParameter("limit"))
            assertEquals("test-api-key", request.header("apiKey"))
        }

    @Test
    fun `getMessages defaults to the client secret and a limit of ten`() =
        runTest {
            val service = createService(code = 200, body = EMPTY_PAGE)

            service.getMessages(wallet = "planter@test", lastSyncTime = "2023-05-01T10:00:00Z", offset = 0)

            val request = lastRequest!!
            assertEquals("10", request.url.queryParameter("limit"))
            assertEquals(BuildConfig.TREETRACKER_CLIENT_SECRET, request.header("apiKey"))
        }

    @Test
    fun `getMessages decodes the response body`() =
        runTest {
            val service = createService(code = 200, body = ONE_MESSAGE_PAGE)

            val response = service.getMessages(apiKey = "key", wallet = "planter@test", lastSyncTime = "since", offset = 0)

            assertEquals(1, response.query.total)
            assertEquals("msg-1", response.messages.single().id)
            assertEquals(MessageType.MESSAGE, response.messages.single().type)
        }

    @Test
    fun `getMessages for a wallet without messages fails with the message the repo treats as no messages`() =
        runTest {
            val service = createService(code = 404, body = "{}", message = "Not Found")

            val error =
                assertFailsWith<HttpException> {
                    service.getMessages(apiKey = "key", wallet = "new@test", lastSyncTime = "since", offset = 0)
                }

            assertEquals(404, error.code())
            assertEquals(Constants.LOCAL_MSG_ERROR_HTTP404, error.localizedMessage)
        }

    private fun createService(
        code: Int,
        body: String,
        message: String = "OK",
    ): MessagesApiService {
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        lastRequest = chain.request()
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(code)
                            .message(message)
                            .body(body.toResponseBody("application/json".toMediaType()))
                            .build()
                    },
                ).build()
        return Retrofit
            .Builder()
            .client(client)
            .baseUrl("https://messages.test/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MessagesApiService::class.java)
    }

    private companion object {
        const val EMPTY_PAGE =
            """{"messages": [], "links": {"prev": null, "next": null}, "query": {"total": 0, "handle": "planter@test", "limit": 10, "offset": 0}}"""

        const val ONE_MESSAGE_PAGE =
            """{"messages": [{"id": "msg-1", "type": "message", "from": "admin", "to": "planter@test", "subject": null,
            "body": "Hi", "composed_at": "2023-05-01T10:00:00.000Z", "parent_message_id": null, "video_link": null,
            "survey_response": null, "survey": null}],
            "links": {"prev": null, "next": null},
            "query": {"total": 1, "handle": "planter@test", "limit": 10, "offset": 0}}"""
    }
}