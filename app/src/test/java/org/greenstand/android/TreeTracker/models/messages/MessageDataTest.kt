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
package org.greenstand.android.TreeTracker.models.messages

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MessageDataTest {
    private val direct =
        DirectMessage(
            id = "direct-1",
            from = "admin",
            to = "planter",
            composedAt = "2023-05-01T10:00:00Z",
            isRead = false,
            parentMessageId = null,
            body = "Hello",
        )

    private val announcement =
        AnnouncementMessage(
            id = "announce-1",
            from = "admin",
            to = "planter",
            composedAt = "2023-05-02T10:00:00Z",
            isRead = true,
            subject = "News",
            body = null,
            videoLink = "https://videos.example/abc",
        )

    private val survey =
        SurveyMessage(
            id = "survey-msg-1",
            from = "admin",
            to = "planter",
            composedAt = "2023-05-03T10:00:00Z",
            isRead = false,
            surveyId = "survey-1",
            title = "Tree health",
            questions = listOf(Question(prompt = "Healthy?", choices = listOf("Yes", "No"))),
            isComplete = false,
        )

    private val surveyResponse =
        SurveyResponseMessage(
            id = "response-1",
            from = "planter",
            to = "admin",
            composedAt = "2023-05-04T10:00:00Z",
            isRead = true,
            surveyId = "survey-1",
            questions = listOf(Question(prompt = "Healthy?", choices = listOf("Yes", "No"))),
            responses = listOf("Yes"),
        )

    @Test
    fun `every message type exposes the common message fields`() {
        val messages: List<Message> = listOf(direct, announcement, survey, surveyResponse)

        assertEquals(listOf("direct-1", "announce-1", "survey-msg-1", "response-1"), messages.map { it.id })
        assertEquals(listOf("admin", "admin", "admin", "planter"), messages.map { it.from })
        assertEquals(listOf("planter", "planter", "planter", "admin"), messages.map { it.to })
        assertEquals(listOf(false, true, false, true), messages.map { it.isRead })
        assertEquals(
            listOf("2023-05-01T10:00:00Z", "2023-05-02T10:00:00Z", "2023-05-03T10:00:00Z", "2023-05-04T10:00:00Z"),
            messages.map { it.composedAt },
        )
    }

    @Test
    fun `messages with the same values are equal`() {
        assertEquals(direct, direct.copy())
        assertEquals(announcement, announcement.copy())
        assertEquals(survey, survey.copy(questions = listOf(Question(prompt = "Healthy?", choices = listOf("Yes", "No")))))
        assertEquals(surveyResponse, surveyResponse.copy())
    }

    @Test
    fun `marking a message read produces a different message`() {
        assertNotEquals(direct, direct.copy(isRead = true))
        assertNotEquals(survey, survey.copy(isComplete = true))
    }

    @Test
    fun `questions compare by prompt and choices`() {
        assertEquals(Question("Q", listOf("A", "B")), Question("Q", listOf("A", "B")))
        assertNotEquals(Question("Q", listOf("A", "B")), Question("Q", listOf("B", "A")))
    }
}