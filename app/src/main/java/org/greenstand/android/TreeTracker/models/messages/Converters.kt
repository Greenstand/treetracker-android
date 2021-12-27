package org.greenstand.android.TreeTracker.models.messages

import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QuestionResponse

fun QuestionResponse.toQuestion(): Question {
    return Question(
        prompt = prompt ?: "NULL PROMPT",
        choices = choices ?: emptyList()
    )
}

fun MessageResponse.toMessage(): Message {
    return when {
        survey.questions.isNotEmpty() && survey.questions.first().prompt != null ->
            return SurveyMessage(
                id = id,
                from = from,
                to = to,
                composedAt = composedAt,
                questions = survey.questions.map { it.toQuestion() },
                answers = survey.answers.filterNotNull()
            )
        else -> TextMessage(
            id = id,
            from = from,
            to = to,
            composedAt = composedAt,
            subject = subject,
            body = body,
            parentMessageId = parentMessageId,
        )
    }
}