package org.greenstand.android.TreeTracker.models.messages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.messages.database.DatabaseConverters
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.QueryResponse
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import timber.log.Timber
import java.util.*

/**
 * For test data, use 'handle3' as a wallet
 */
class MessagesRepo(
    private val apiService: MessagesApiService,
    private val users: Users,
    private val timeProvider: TimeProvider,
    private val messagesDao: MessagesDAO,
    private val messageUploader: MessageUploader,
) {

    suspend fun saveMessage(wallet: String, to: String, body: String) {
        messagesDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                wallet = wallet,
                type = MessageType.MESSAGE,
                from = wallet,
                to = to,
                subject = null,
                body = body,
                composedAt = timeProvider.currentTime().toString(),
                parentMessageId = null,
                videoLink = null,
                surveyResponse = null,
                shouldUpload = true,
                bundleId = null,
                isRead = true
            )
        )
    }

    // WIP
    suspend fun saveSurveyAnswers(wallet: String, to: String, surveyResponse: List<String>) {
        messagesDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                wallet = wallet,
                type = MessageType.SURVEY_RESPONSE,
                from = wallet,
                to = to,
                subject = null,
                body = null,
                composedAt = timeProvider.currentTime().toString(),
                parentMessageId = null,
                videoLink = null,
                surveyResponse = surveyResponse,
                shouldUpload = true,
                bundleId = null,
                isRead = true,
            )
        )
    }

    suspend fun getMessages(wallet: String): List<Message> = withContext(Dispatchers.IO) {
        return@withContext messagesDao.getMessagesForWallet(wallet)
            .map { convertMessageEntityToMessage(it) }
    }

    fun getDirectMessages(
        wallet: String,
        otherChatIdentifier: String): Flow<List<DirectMessage>> {
        return messagesDao.getDirectMessagesForWallet(wallet)
            .map { messages ->
                messages
                    .map { convertMessageEntityToMessage(it) }
                    .filterIsInstance<DirectMessage>()
                    .filter { (it.from == wallet || it.from == otherChatIdentifier) && (it.to == otherChatIdentifier || it.to == wallet) }
                    .sortedByDescending { it.composedAt }
            }
    }

    suspend fun getSurveyMessage(id: String): SurveyMessage {
        return convertMessageEntityToMessage(messagesDao.getMessage(id)!!) as SurveyMessage
    }

    /**
     * When uploading trees, messages will be synced locally by this method
     */
    suspend fun syncMessages() {
        for(wallet in users.getUserList().map { it.wallet }) {
            try {
                getMessagesForWallet(wallet)
            } catch (e: Exception) {
                if (e.localizedMessage == "HTTP 404 Not Found") {
                    // 404 indicates the user has never had messages before
                    continue
                } else {
                    Timber.e(e)
                    throw e
                }
            }
        }
        messageUploader.uploadMessages()
    }

    private suspend fun getMessagesForWallet(wallet: String) {
        val lastSyncTime = getLastSyncTime(wallet)
        var query = QueryResponse(
            handle = wallet,
            limit = 5,
            offset = 0,
            total = -1,
        )
        var result = apiService.getMessages(
            wallet = wallet,
            lastSyncTime = lastSyncTime,
            offset = query.offset,
            limit = query.limit,
        )
        query = result.query

        if (query.total == 0) return

        result.messages.forEach { saveMessageResponse(wallet, it) }
        while (query.total >= query.limit + query.offset) {
            query = result.query.copy(offset = result.query.offset + result.query.limit)
            result = apiService.getMessages(
                wallet = wallet,
                lastSyncTime = lastSyncTime,
                offset = query.offset,
                limit = query.limit,
            )
            result.messages.forEach { saveMessageResponse(wallet, it) }
        }
    }

    private suspend fun saveMessageResponse(wallet: String, message: MessageResponse) {
        if (message.type == MessageType.SURVEY_RESPONSE) return

        val messageEntity = with(message) {
            MessageEntity(
                id = id,
                wallet = wallet,
                type = type,
                from = from,
                to = to,
                subject = subject,
                body = body,
                composedAt = composedAt,
                parentMessageId = parentMessageId,
                videoLink = videoLink,
                surveyResponse = null,
                shouldUpload = false,
                bundleId = null,
                isRead = false,
            )
        }
        messagesDao.insertMessage(messageEntity)

        // If there is no survey, don't continue on
        message.survey ?: return

        val surveyEntity = with(message.survey) {
            SurveyEntity(
                id = id,
                messageId = messageEntity.id,
                title = title,
            )
        }
        messagesDao.insertSurvey(surveyEntity)

        message.survey.questions.map { question ->
            val questionEntity = QuestionEntity(
                surveyId = message.survey.id,
                prompt = question.prompt,
                choices = question.choices,
            )
            messagesDao.insertQuestion(questionEntity)
        }
        Timber.tag("JONATHAN").d("Saved survey")
    }

    private suspend fun convertMessageEntityToMessage(messageEntity: MessageEntity): Message {
        return messagesDao.getSurveyForMessage(messageEntity.id)?.let { surveyEntity ->
            val questionEntities = messagesDao.getQuestionsForSurvey(surveyEntity.id)
            DatabaseConverters.createMessageFromEntities(messageEntity, surveyEntity, questionEntities)
        } ?: DatabaseConverters.createMessageFromEntities(messageEntity, null, null)
    }

    private suspend fun getLastSyncTime(wallet: String): String {
        return messagesDao.getLatestSyncTimeForWallet(wallet)
            ?: Instant.fromEpochMilliseconds(0).toString()
    }
}