package org.greenstand.android.TreeTracker.models.messages

import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType

object DatabaseConverters {

    fun createMessageResponseFromEntities(
        messageEntity: MessageEntity,
        surveyEntity: SurveyEntity? = null,
        questionEntities: List<QuestionEntity>? = null): MessageResponse? {
        return when(messageEntity.type) {
            MessageType.MESSAGE ->
                MessageResponse(
                    id = messageEntity.id,
                    type = messageEntity.type,
                    from = messageEntity.from,
                    to = messageEntity.to,
                    subject = messageEntity.subject,
                    body = messageEntity.body,
                    composedAt = messageEntity.composedAt,
                    parentMessageId = messageEntity.parentMessageId,
                    videoLink = messageEntity.videoLink,
                    surveyResponse = null,
                    survey = null,
                )
            MessageType.SURVEY_RESPONSE ->
                MessageResponse(
                    id = messageEntity.id,
                    type = messageEntity.type,
                    from = messageEntity.from,
                    to = messageEntity.to,
                    subject = messageEntity.subject,
                    body = messageEntity.body,
                    composedAt = messageEntity.composedAt,
                    parentMessageId = messageEntity.parentMessageId,
                    videoLink = messageEntity.videoLink,
                    surveyResponse = null,
                    survey = null,
                )
            MessageType.ANNOUNCE,
            MessageType.SURVEY -> null
        }
    }

    fun createMessageFromEntities(
        messageEntity: MessageEntity,
        surveyEntity: SurveyEntity? = null,
        questionEntities: List<QuestionEntity>? = null): Message {

        return when(messageEntity.type) {
            MessageType.MESSAGE ->
                DirectMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.from,
                    composedAt = messageEntity.composedAt,
                    parentMessageId = messageEntity.parentMessageId,
                    body = messageEntity.body ?: "",
                )
            MessageType.ANNOUNCE ->
                AnnouncementMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.from,
                    composedAt = messageEntity.composedAt,
                    subject = messageEntity.subject ?: "",
                    body = messageEntity.body ?: "",
                )
            MessageType.SURVEY ->
                SurveyMessage(
                    id = messageEntity.id,
                    from = messageEntity.from,
                    to = messageEntity.from,
                    composedAt = messageEntity.composedAt,
                    title = surveyEntity!!.title,
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
                    to = messageEntity.from,
                    composedAt = messageEntity.composedAt,
                    responses = messageEntity.surveyResponse ?: emptyList(),
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