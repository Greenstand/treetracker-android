package org.greenstand.android.TreeTracker.models.messages

data class Message(
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
    val composedAt: String,
    val parentMessageId: String?,
    val videoLink: String?,
    val survey: Survey?,
)

data class Survey(
    val questions: List<Question>?,
    val answers: List<String>?,
)

data class Question(
    val prompt: String?,
    val choices: List<String>?,
)