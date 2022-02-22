package org.greenstand.android.TreeTracker.models.messages.network.responses

class SurveyResponse(
    val id: String?,
    val title: String?,
    val response: Boolean,
    val questions: List<QuestionResponse>,
    val answers: List<String?>,
)