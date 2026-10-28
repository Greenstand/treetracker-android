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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.models.messages.network.responses.LinksResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QueryResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QuestionResponse
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MessagesSerializationTest {
    // Same configuration as the app's Json instance (see AppModule)
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Test
    fun `messages response decodes a realistic server payload`() {
        val response = json.decodeFromString<MessagesResponse>(MESSAGES_PAYLOAD)

        assertEquals(QueryResponse(total = 3, handle = "planter@test", limit = 10, offset = 0), response.query)
        assertEquals(LinksResponse(prev = null, next = "/messaging/message?handle=planter@test&offset=10"), response.links)
        assertEquals(3, response.messages.size)

        val direct = response.messages[0]
        assertEquals("msg-1", direct.id)
        assertEquals(MessageType.MESSAGE, direct.type)
        assertEquals("admin", direct.from)
        assertEquals("planter@test", direct.to)
        assertEquals("Hello planter", direct.body)
        assertEquals("2023-05-01T10:00:00.000Z", direct.composedAt)
        assertEquals("msg-0", direct.parentMessageId)
        assertNull(direct.survey)

        val announcement = response.messages[1]
        assertEquals(MessageType.ANNOUNCE, announcement.type)
        assertEquals("Planting season", announcement.subject)
        assertEquals("https://videos.example/abc", announcement.videoLink)
    }

    @Test
    fun `messages response decodes surveys with their questions`() {
        val survey = json.decodeFromString<MessagesResponse>(MESSAGES_PAYLOAD).messages[2]

        assertEquals(MessageType.SURVEY, survey.type)
        assertEquals("survey-1", survey.survey!!.id)
        assertEquals("Tree health", survey.survey!!.title)
        assertEquals(
            listOf(
                QuestionResponse(prompt = "Are the trees healthy?", choices = listOf("Yes", "No")),
                QuestionResponse(prompt = "How many died?", choices = listOf("0", "1-5", "More")),
            ),
            survey.survey!!.questions,
        )
    }

    @Test
    fun `message response ignores unknown fields`() {
        val message =
            json.decodeFromString<MessageResponse>(
                messageJson(type = "message", extra = "\"unexpected\": 42,"),
            )

        assertEquals("msg-x", message.id)
    }

    @Test
    fun `message response with an unknown type fails to decode`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MessageResponse>(messageJson(type = "poll"))
        }
    }

    @Test
    fun `message response missing a nullable field fails to decode with the app configuration`() {
        val payload =
            """
            {"id": "msg-x", "type": "message", "from": "admin", "to": "planter",
             "body": "Hi", "composed_at": "2023-05-01T10:00:00.000Z", "parent_message_id": null,
             "video_link": null, "survey_response": null, "survey": null}
            """.trimIndent()

        assertFailsWith<SerializationException> {
            json.decodeFromString<MessageResponse>(payload)
        }
    }

    @Test
    fun `message type serial names map to enum values`() {
        val expected =
            mapOf(
                "message" to MessageType.MESSAGE,
                "announce" to MessageType.ANNOUNCE,
                "survey" to MessageType.SURVEY,
                "survey_response" to MessageType.SURVEY_RESPONSE,
            )

        expected.forEach { (serialName, type) ->
            assertEquals(type, json.decodeFromString<MessageType>("\"$serialName\""))
            assertEquals("\"$serialName\"", json.encodeToString(type))
        }
    }

    @Test
    fun `message request encodes with the api field names and explicit nulls`() {
        val request =
            MessageRequest(
                id = "msg-1",
                parentMessageId = null,
                recipientHandle = "admin",
                authorHandle = "planter@test",
                type = "message",
                body = "Hello",
                surveyResponse = null,
                surveyId = null,
                composedAt = "2023-05-01T10:00:00Z",
            )

        val encoded = json.parseToJsonElement(json.encodeToString(request)).jsonObject

        assertEquals(
            setOf(
                "id",
                "parentMessageId",
                "recipient_handle",
                "author_handle",
                "type",
                "body",
                "survey_response",
                "survey_id",
                "composed_at",
            ),
            encoded.keys,
        )
        assertEquals("admin", encoded["recipient_handle"]!!.jsonPrimitive.content)
        assertEquals("planter@test", encoded["author_handle"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, encoded["parentMessageId"])
        assertEquals(JsonNull, encoded["survey_response"])
    }

    @Test
    fun `message request with survey answers round trips`() {
        val request =
            MessageRequest(
                id = "msg-2",
                parentMessageId = "msg-1",
                recipientHandle = "admin",
                authorHandle = "planter@test",
                type = "survey_response",
                body = null,
                surveyResponse = listOf("Yes", "1-5"),
                surveyId = "survey-1",
                composedAt = "2023-05-01T10:00:00Z",
            )

        val encoded = json.encodeToString(request)

        assertEquals(request, json.decodeFromString<MessageRequest>(encoded))
        assertEquals(
            listOf("Yes", "1-5"),
            json
                .parseToJsonElement(encoded)
                .jsonObject["survey_response"]!!
                .jsonArray
                .map { it.jsonPrimitive.content },
        )
    }

    private fun messageJson(
        type: String,
        extra: String = "",
    ): String =
        """
        {$extra "id": "msg-x", "type": "$type", "from": "admin", "to": "planter", "subject": null,
         "body": "Hi", "composed_at": "2023-05-01T10:00:00.000Z", "parent_message_id": null,
         "video_link": null, "survey_response": null, "survey": null}
        """.trimIndent()

    private companion object {
        val MESSAGES_PAYLOAD =
            """
            {
              "messages": [
                {
                  "id": "msg-1",
                  "type": "message",
                  "from": "admin",
                  "to": "planter@test",
                  "subject": null,
                  "body": "Hello planter",
                  "composed_at": "2023-05-01T10:00:00.000Z",
                  "parent_message_id": "msg-0",
                  "video_link": null,
                  "survey_response": null,
                  "survey": null
                },
                {
                  "id": "msg-2",
                  "type": "announce",
                  "from": "admin",
                  "to": "planter@test",
                  "subject": "Planting season",
                  "body": "It starts next week",
                  "composed_at": "2023-05-02T10:00:00.000Z",
                  "parent_message_id": null,
                  "video_link": "https://videos.example/abc",
                  "survey_response": null,
                  "survey": null
                },
                {
                  "id": "msg-3",
                  "type": "survey",
                  "from": "admin",
                  "to": "planter@test",
                  "subject": "Survey",
                  "body": null,
                  "composed_at": "2023-05-03T10:00:00.000Z",
                  "parent_message_id": null,
                  "video_link": null,
                  "survey_response": null,
                  "survey": {
                    "id": "survey-1",
                    "title": "Tree health",
                    "questions": [
                      {"prompt": "Are the trees healthy?", "choices": ["Yes", "No"]},
                      {"prompt": "How many died?", "choices": ["0", "1-5", "More"]}
                    ]
                  }
                }
              ],
              "links": {"prev": null, "next": "/messaging/message?handle=planter@test&offset=10"},
              "query": {"total": 3, "handle": "planter@test", "limit": 10, "offset": 0}
            }
            """.trimIndent()
    }
}