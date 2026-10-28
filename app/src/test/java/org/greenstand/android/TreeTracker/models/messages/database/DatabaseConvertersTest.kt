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
package org.greenstand.android.TreeTracker.models.messages.database

import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.messages.SurveyResponseMessage
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseConvertersTest {
    private fun entity(
        type: MessageType,
        subject: String? = "subject",
        body: String? = "body",
        surveyResponse: List<String>? = null,
        surveyId: String? = null,
        isSurveyComplete: Boolean? = null,
        isRead: Boolean = false,
        videoLink: String? = null,
    ) = MessageEntity(
        id = "message-id",
        wallet = "wallet",
        type = type,
        from = "author",
        to = "recipient",
        subject = subject,
        body = body,
        composedAt = "2024-03-01T10:00:00Z",
        parentMessageId = "parent-id",
        videoLink = videoLink,
        surveyResponse = surveyResponse,
        shouldUpload = true,
        bundleId = null,
        isRead = isRead,
        surveyId = surveyId,
        isSurveyComplete = isSurveyComplete,
    )

    private val survey = SurveyEntity(id = "survey-id", title = "Tree health")

    private val questions =
        listOf(
            QuestionEntity(surveyId = "survey-id", prompt = "Healthy?", choices = listOf("yes", "no")),
            QuestionEntity(surveyId = "survey-id", prompt = "Height?", choices = listOf("<1m", ">1m")),
        )

    private val expectedQuestions =
        listOf(
            Question(prompt = "Healthy?", choices = listOf("yes", "no")),
            Question(prompt = "Height?", choices = listOf("<1m", ">1m")),
        )

    // region createMessageRequestFromEntities

    @Test
    fun `direct message becomes a message request without survey fields`() {
        val request =
            DatabaseConverters.createMessageRequestFromEntities(
                entity(MessageType.MESSAGE, surveyResponse = listOf("ignored"), surveyId = "ignored"),
            )

        assertEquals(
            MessageRequest(
                id = "message-id",
                parentMessageId = "parent-id",
                recipientHandle = "recipient",
                authorHandle = "author",
                type = "message",
                body = "body",
                surveyResponse = null,
                surveyId = null,
                composedAt = "2024-03-01T10:00:00Z",
            ),
            request,
        )
    }

    @Test
    fun `direct message request keeps a null body`() {
        val request = DatabaseConverters.createMessageRequestFromEntities(entity(MessageType.MESSAGE, body = null))

        assertNull(request.body)
    }

    @Test
    fun `survey response becomes a survey_response request with its answers`() {
        val request =
            DatabaseConverters.createMessageRequestFromEntities(
                entity(MessageType.SURVEY_RESPONSE, surveyResponse = listOf("yes", ">1m"), surveyId = "survey-id"),
            )

        assertEquals(
            MessageRequest(
                id = "message-id",
                parentMessageId = "parent-id",
                recipientHandle = "recipient",
                authorHandle = "author",
                type = "survey_response",
                body = "body",
                surveyResponse = listOf("yes", ">1m"),
                surveyId = "survey-id",
                composedAt = "2024-03-01T10:00:00Z",
            ),
            request,
        )
    }

    @Test
    fun `announcement cannot be converted to a request`() {
        val error =
            assertThrows(IllegalStateException::class.java) {
                DatabaseConverters.createMessageRequestFromEntities(entity(MessageType.ANNOUNCE))
            }

        assertTrue(error.message!!.contains("ANNOUNCE"))
    }

    @Test
    fun `survey cannot be converted to a request`() {
        val error =
            assertThrows(IllegalStateException::class.java) {
                DatabaseConverters.createMessageRequestFromEntities(entity(MessageType.SURVEY))
            }

        assertTrue(error.message!!.contains("SURVEY"))
    }

    // endregion

    // region createMessageFromEntities

    @Test
    fun `direct message entity becomes a DirectMessage`() {
        val message = DatabaseConverters.createMessageFromEntities(entity(MessageType.MESSAGE, isRead = true))

        assertEquals(
            DirectMessage(
                id = "message-id",
                from = "author",
                to = "recipient",
                composedAt = "2024-03-01T10:00:00Z",
                isRead = true,
                parentMessageId = "parent-id",
                body = "body",
            ),
            message,
        )
    }

    @Test
    fun `direct message with a null body gets an empty body`() {
        val message = DatabaseConverters.createMessageFromEntities(entity(MessageType.MESSAGE, body = null)) as DirectMessage

        assertEquals("", message.body)
    }

    @Test
    fun `announcement entity becomes an AnnouncementMessage`() {
        val message =
            DatabaseConverters.createMessageFromEntities(
                entity(MessageType.ANNOUNCE, videoLink = "https://video"),
            )

        assertEquals(
            AnnouncementMessage(
                id = "message-id",
                from = "author",
                to = "recipient",
                composedAt = "2024-03-01T10:00:00Z",
                isRead = false,
                subject = "subject",
                body = "body",
                videoLink = "https://video",
            ),
            message,
        )
    }

    @Test
    fun `announcement with null subject and body gets empty strings`() {
        val message =
            DatabaseConverters.createMessageFromEntities(
                entity(MessageType.ANNOUNCE, subject = null, body = null),
            ) as AnnouncementMessage

        assertEquals("", message.subject)
        assertEquals("", message.body)
        assertNull(message.videoLink)
    }

    @Test
    fun `survey entity becomes a SurveyMessage with the survey title and questions`() {
        val message =
            DatabaseConverters.createMessageFromEntities(
                entity(MessageType.SURVEY, isSurveyComplete = true),
                survey,
                questions,
            )

        assertEquals(
            SurveyMessage(
                id = "message-id",
                from = "author",
                to = "recipient",
                composedAt = "2024-03-01T10:00:00Z",
                isRead = false,
                surveyId = "survey-id",
                title = "Tree health",
                questions = expectedQuestions,
                isComplete = true,
            ),
            message,
        )
    }

    @Test
    fun `survey with unknown completion and no questions is incomplete and empty`() {
        val message =
            DatabaseConverters.createMessageFromEntities(
                entity(MessageType.SURVEY, isSurveyComplete = null),
                survey,
                null,
            ) as SurveyMessage

        assertEquals(false, message.isComplete)
        assertEquals(emptyList<Question>(), message.questions)
    }

    @Test
    fun `survey without its survey entity throws`() {
        assertThrows(NullPointerException::class.java) {
            DatabaseConverters.createMessageFromEntities(entity(MessageType.SURVEY))
        }
    }

    @Test
    fun `survey response entity becomes a SurveyResponseMessage`() {
        val message =
            DatabaseConverters.createMessageFromEntities(
                entity(MessageType.SURVEY_RESPONSE, surveyResponse = listOf("yes", ">1m"), isRead = true),
                survey,
                questions,
            )

        assertEquals(
            SurveyResponseMessage(
                id = "message-id",
                from = "author",
                to = "recipient",
                composedAt = "2024-03-01T10:00:00Z",
                isRead = true,
                surveyId = "survey-id",
                questions = expectedQuestions,
                responses = listOf("yes", ">1m"),
            ),
            message,
        )
    }

    @Test
    fun `survey response without answers or questions gets empty lists`() {
        val message =
            DatabaseConverters.createMessageFromEntities(
                entity(MessageType.SURVEY_RESPONSE, surveyResponse = null),
                survey,
                null,
            ) as SurveyResponseMessage

        assertEquals(emptyList<String>(), message.responses)
        assertEquals(emptyList<Question>(), message.questions)
    }

    @Test
    fun `survey response without its survey entity throws`() {
        assertThrows(NullPointerException::class.java) {
            DatabaseConverters.createMessageFromEntities(entity(MessageType.SURVEY_RESPONSE), null, questions)
        }
    }

    // endregion
}