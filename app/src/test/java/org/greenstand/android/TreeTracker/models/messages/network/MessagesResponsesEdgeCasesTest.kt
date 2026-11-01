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
import kotlinx.serialization.json.jsonObject
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.models.messages.network.responses.LinksResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.SurveyResponse
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Edge payloads for the messaging API models. MessagesSerializationTest covers the realistic payloads. */
class MessagesResponsesEdgeCasesTest {
    // Same configuration as the app's Json instance (see AppModule)
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private fun messageJson(
        type: String = "message",
        surveyResponse: String = "null",
        survey: String = "null",
    ) = """
        {"id": "msg-x", "type": "$type", "from": "admin", "to": "planter", "subject": null,
         "body": null, "composed_at": "2023-05-01T10:00:00.000Z", "parent_message_id": null,
         "video_link": null, "survey_response": $surveyResponse, "survey": $survey}
        """.trimIndent()

    @Test
    fun `an empty page decodes with no messages and no links`() {
        val response =
            json.decodeFromString<MessagesResponse>(
                """
                {"messages": [], "links": {"prev": null, "next": null},
                 "query": {"total": 0, "handle": "planter@test", "limit": 100, "offset": 0}}
                """.trimIndent(),
            )

        assertTrue(response.messages.isEmpty())
        assertEquals(LinksResponse(prev = null, next = null), response.links)
        assertEquals(0, response.query.total)
    }

    @Test
    fun `a page without the query block fails to decode`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MessagesResponse>("""{"messages": [], "links": {"prev": null, "next": null}}""")
        }
    }

    @Test
    fun `a survey response message decodes its answers`() {
        val message =
            json.decodeFromString<MessageResponse>(
                messageJson(type = "survey_response", surveyResponse = """["Yes", "1-5"]"""),
            )

        assertEquals(MessageType.SURVEY_RESPONSE, message.type)
        assertEquals(listOf("Yes", "1-5"), message.surveyResponse)
        assertNull(message.body)
    }

    @Test
    fun `a survey without questions decodes with an empty question list`() {
        val message =
            json.decodeFromString<MessageResponse>(
                messageJson(type = "survey", survey = """{"id": "survey-1", "title": "Empty", "questions": []}"""),
            )

        val survey: SurveyResponse = message.survey!!
        assertEquals("survey-1", survey.id)
        assertTrue(survey.questions.isEmpty())
    }

    @Test
    fun `a survey without its questions field fails to decode`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MessageResponse>(messageJson(type = "survey", survey = """{"id": "survey-1", "title": "No questions"}"""))
        }
    }

    @Test
    fun `message type serial names are case sensitive`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MessageResponse>(messageJson(type = "MESSAGE"))
        }
    }

    @Test
    fun `a null message type fails to decode`() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<MessageResponse>(messageJson().replace("\"type\": \"message\"", "\"type\": null"))
        }
    }

    @Test
    fun `message request decodes when the server echoes extra fields`() {
        val decoded =
            json.decodeFromString<MessageRequest>(
                """
                {"id": "msg-1", "parentMessageId": null, "recipient_handle": "admin", "author_handle": "planter",
                 "type": "message", "body": "Hi", "survey_response": null, "survey_id": null,
                 "composed_at": "2023-05-01T10:00:00Z", "bundle_id": "ignored"}
                """.trimIndent(),
            )

        assertEquals("msg-1", decoded.id)
        assertEquals("planter", decoded.authorHandle)
    }

    @Test
    fun `message request with a snake case parent id key does not fill parentMessageId`() {
        val result =
            runCatching {
                json.decodeFromString<MessageRequest>(
                    """
                    {"id": "msg-1", "parent_message_id": "msg-0", "recipient_handle": "admin", "author_handle": "planter",
                     "type": "message", "body": "Hi", "survey_response": null, "survey_id": null,
                     "composed_at": "2023-05-01T10:00:00Z"}
                    """.trimIndent(),
                )
            }

        // The request's parent id key is camelCase (unlike the response's), so the snake case key is unknown and
        // the required parentMessageId is missing.
        assertTrue(result.exceptionOrNull() is SerializationException)
    }

    @Test
    fun `message request encodes an empty survey response as an empty array`() {
        val request =
            MessageRequest(
                id = "msg-1",
                parentMessageId = null,
                recipientHandle = "admin",
                authorHandle = "planter",
                type = "survey_response",
                body = null,
                surveyResponse = emptyList(),
                surveyId = "survey-1",
                composedAt = "2023-05-01T10:00:00Z",
            )

        val encoded = json.parseToJsonElement(json.encodeToString(request)).jsonObject

        assertEquals("[]", encoded["survey_response"].toString())
        assertEquals("\"survey-1\"", encoded["survey_id"].toString())
    }
}