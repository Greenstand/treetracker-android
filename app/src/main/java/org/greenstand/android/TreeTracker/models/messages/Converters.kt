package org.greenstand.android.TreeTracker.models.messages

import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QuestionResponse

fun QuestionResponse.toQuestion(): Question {
    return Question(
        prompt = prompt!!,
        choices = choices!!
    )
}

fun MessageResponse.toMessage(): Message {
    return when {
        survey.questions != null -> return SurveyMessage(
            from = from,
            to = to,
            composedAt = composedAt,
            questions = survey.questions.map { it.toQuestion() },
            answers = survey.answers!!
        )
        else -> TextMessage(
            from = from,
            to = to,
            composedAt = composedAt,
            subject = subject,
            body = body,
            parentMessageId = parentMessageId,
        )
    }
}