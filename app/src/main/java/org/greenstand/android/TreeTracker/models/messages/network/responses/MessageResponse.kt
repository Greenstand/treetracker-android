package org.greenstand.android.TreeTracker.models.messages.network.responses

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    val id: String,
    val type: MessageType,
    val from: String,
    val to: String,
    val subject: String?,
    val body: String?,
    @SerializedName("composed_at")
    val composedAt: String,
    @SerializedName("parent_message_id")
    val parentMessageId: String?,
    @SerializedName("video_link")
    val videoLink: String?,
    @SerializedName("survey_response")
    val surveyResponse: List<String>?,
    val survey: SurveyResponse?,
)