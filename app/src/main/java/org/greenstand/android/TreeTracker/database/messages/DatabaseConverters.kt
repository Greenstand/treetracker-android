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
package org.greenstand.android.TreeTracker.database.messages

import org.greenstand.android.TreeTracker.database.messages.entities.MessageEntity
import org.greenstand.android.TreeTracker.database.messages.entities.QuestionEntity
import org.greenstand.android.TreeTracker.database.messages.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.DirectMessage
import org.greenstand.android.TreeTracker.models.messages.Message
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.messages.SurveyResponseMessage
import org.greenstand.android.TreeTracker.models.messages.network.requests.MessageRequest
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType

object DatabaseConverters {

    fun createMessageRequestFromEntities(messageEntity: MessageEntity): MessageRequest {
        return when (messageEntity.type) {
            MessageType.MESSAGE ->
                MessageRequest(
                    id = messageEntity.id,
                    type = "message",
                    authorHandle = messageEntity.from,
                    recipientHandle = messageEntity.to,
                    body = messageEntity.body,
                    composedAt = messageEntity.composedAt,
                    parentMessageId = messageEntity.parentMessageId,
                    surveyResponse = null,
                    surveyId = null,
                )
            MessageType.SURVEY_RESPONSE ->
                MessageRequest(
                    id = messageEntity.id,
                    type = "survey_response",
                    authorHandle = messageEntity.from,
                    recipientHandle = messageEntity.to,
                    body = messageEntity.body,
                    composedAt = messageEntity.composedAt,
                    parentMessageId = messageEntity.parentMessageId,
                    surveyResponse = messageEntity.surveyResponse,
                    surveyId = messageEntity.surveyId,
                )
            MessageType.ANNOUNCE,
            MessageType.SURVEY ->
                throw IllegalStateException(
                    "Invalid message type ${messageEntity.type} is being created for upload"
                )
        }
    }

    fun createMessageFromEntities(
        messageEntity: MessageEntity,
        surveyEntity: SurveyEntity? = null,
        questionEntities: List<QuestionEntity>? = null
    ): Message {

        return when (messageEntity.type) {
            MessageType.MESSAGE ->
                DirectMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.to,
                    composedAt = messageEntity.composedAt,
                    parentMessageId = messageEntity.parentMessageId,
                    body = messageEntity.body ?: "",
                    isRead = messageEntity.isRead,
                )
            MessageType.ANNOUNCE ->
                AnnouncementMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.to,
                    composedAt = messageEntity.composedAt,
                    subject = messageEntity.subject ?: "",
                    body = messageEntity.body ?: "",
                    isRead = messageEntity.isRead,
                    videoLink = messageEntity.videoLink,
                )
            MessageType.SURVEY ->
                SurveyMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.to,
                    composedAt = messageEntity.composedAt,
                    title = surveyEntity!!.title,
                    isRead = messageEntity.isRead,
                    surveyId = surveyEntity.id,
                    isComplete = messageEntity.isSurveyComplete ?: false,
                    questions = questionEntities?.map {
                        Question(
                            prompt = it.prompt,
                            choices = it.choices
                        )
                    } ?: emptyList()
                )
            MessageType.SURVEY_RESPONSE ->
                SurveyResponseMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.to,
                    composedAt = messageEntity.composedAt,
                    responses = messageEntity.surveyResponse ?: emptyList(),
                    isRead = messageEntity.isRead,
                    surveyId = surveyEntity!!.id,
                    questions = questionEntities?.map {
                        Question(
                            prompt = it.prompt,
                            choices = it.choices
                        )
                    } ?: emptyList()
                )
        }
    }
}