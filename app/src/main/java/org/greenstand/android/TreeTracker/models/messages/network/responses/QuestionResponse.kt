package org.greenstand.android.TreeTracker.models.messages.network.responses

data class QuestionResponse(
    val prompt: String,
    val choices: List<String>,
)