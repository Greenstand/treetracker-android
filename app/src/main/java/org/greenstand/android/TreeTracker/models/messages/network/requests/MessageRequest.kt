package org.greenstand.android.TreeTracker.models.messages.network.requests

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    val id: String,
    val parentMessageId: String?,
    @SerializedName("recipient_handle")
    val recipientHandle: String,
    @SerializedName("author_handle")
    val authorHandle: String,
    val type: String,
    val body: String?,
    @SerializedName("survey_response")
    val surveyResponse: List<String>?,
    @SerializedName("survey_id")
    val surveyId: String?,
    @SerializedName("composed_at")
    val composedAt: String,
)