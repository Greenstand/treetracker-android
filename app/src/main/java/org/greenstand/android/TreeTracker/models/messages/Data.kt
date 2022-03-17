package org.greenstand.android.TreeTracker.models.messages

interface Message {
    val id: String
    val from: String
    val to: String
    val composedAt: String
    val isRead: Boolean
}

data class DirectMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val parentMessageId: String?,
    val body: String,
) : Message

data class AnnouncementMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val subject: String,
    val body: String?,
) : Message

data class SurveyMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val surveyId: String,
    val title: String,
    val questions: List<Question>,
    val isComplete: Boolean,
) : Message

data class SurveyResponseMessage(
    override val id: String,
    override val from: String,
    override val to: String,
    override val composedAt: String,
    override val isRead: Boolean,
    val surveyId: String,
    val questions: List<Question>,
    val responses: List<String>,
) : Message

data class Question(
    val prompt: String,
    val choices: List<String>,
)