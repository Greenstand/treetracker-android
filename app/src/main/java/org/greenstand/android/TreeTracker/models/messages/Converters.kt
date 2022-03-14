package org.greenstand.android.TreeTracker.models.messages

import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.QuestionResponse
import timber.log.Timber


fun QuestionResponse.toQuestion(): Question {
    return Question(
        prompt = prompt,
        choices = choices
    )
}

fun MessageResponse.toMessage(): Message {
    return when(type) {
        MessageType.MESSAGE ->
            DirectMessage(
                id = id,
                from = from,
                to = to,
                composedAt = composedAt,
                parentMessageId = parentMessageId,
                body = checkNotNull(body) { Timber.e("Body cannot be null for DirectMessage.") },
                isRead = false,
            )
        MessageType.ANNOUNCE ->
            AnnouncementMessage(
                id = id,
                from = from,
                to = to,
                composedAt = composedAt,
                subject = checkNotNull(subject) { Timber.e("Subject cannot be null for AnnouncementMessage.") },
                body = body,
                isRead = false,
            )
        MessageType.SURVEY ->
            return SurveyMessage(
                id = id,
                from = from,
                to = to,
                composedAt = composedAt,
                title = survey!!.title,
                questions = survey.questions.map { it.toQuestion() },
                isRead = false,
            )
        MessageType.SURVEY_RESPONSE ->
            return SurveyResponseMessage(
                id = id,
                from = from,
                to = to,
                composedAt = composedAt,
                questions = survey!!.questions.map { it.toQuestion() },
                responses = surveyResponse!!,
                isRead = false,
            )
    }
}