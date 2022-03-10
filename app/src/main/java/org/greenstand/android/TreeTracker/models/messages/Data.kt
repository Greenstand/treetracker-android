package org.greenstand.android.TreeTracker.models.messages

interface Message {
    val id: String
    val from: String
    val to: String
    val composedAt: String
}

data class TextMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    val subject: String,
    val body: String,
    val parentMessageId: String?,
) : Message

data class SurveyMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    val questions: List<Question>,
    val answers: List<String>,
) : Message

data class Question(
    val prompt: String,
    val choices: List<String>,
)

data class GeneralMessageItem(
    val id: String,
    val details: String,
    val MessageType: String,
    //Message Type determines the icon and text that would be shown for that type of Individual Message Item
    )