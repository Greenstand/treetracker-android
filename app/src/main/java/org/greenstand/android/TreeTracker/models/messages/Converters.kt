package org.greenstand.android.TreeTracker.models.messages

import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QuestionResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.SurveyResponse

fun QuestionResponse.toQuestions(): Question {
    return Question(
        prompt = prompt,
        choices = choices
    )
}

fun SurveyResponse.toSurvey(): Survey {
    return Survey(
        questions = questions?.map { it.toQuestions() },
        answers = answers,
    )
}

fun MessageResponse.toMessage(): Message {
    return Message(
        from = from,
        to = to,
        subject = subject,
        body = body,
        composedAt = composedAt,
        parentMessageId = parentMessageId,
        videoLink = videoLink,
        survey = survey.toSurvey()
    )
}