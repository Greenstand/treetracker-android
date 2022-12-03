package org.greenstand.android.TreeTracker.models.messages

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.database.DatabaseConverters
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.models.messages.database.entities.MessageEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.QuestionEntity
import org.greenstand.android.TreeTracker.models.messages.database.entities.SurveyEntity
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessagesResponse
import org.greenstand.android.TreeTracker.models.messages.network.responses.QueryResponse
import org.greenstand.android.TreeTracker.utilities.Constants
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.utils.runInParallel
import timber.log.Timber
import java.util.*

/**
 * For test data, use 'handle2@test', 'handle3@test', or 'handle4@test' as a wallet (email)
 */
class MessagesRepo(
    private val apiService: MessagesApiService,
    private val userRepo: UserRepo,
    private val timeProvider: TimeProvider,
    private val messagesDao: MessagesDAO,
    private val messageUploader: MessageUploader,
) {

    suspend fun markMessageAsRead(messageId: String) {
        messagesDao.markMessageAsRead(listOf(messageId))
    }

    suspend fun markMessagesAsRead(messageIds: List<String>) {
        messagesDao.markMessageAsRead(messageIds)
    }

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
                isRead = true,
                surveyId = null,
                isSurveyComplete = null,
            )
        )
    }

    suspend fun saveSurveyAnswers(messageId: String, surveyResponse: List<String>) {
        val surveyMessage = messagesDao.getMessage(messageId)!!
        // make messages point to surveys to have survey and response point to same survey
        messagesDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                wallet = surveyMessage.to,
                type = MessageType.SURVEY_RESPONSE,
                from = surveyMessage.to,
                to = surveyMessage.from,
                subject = null,
                body = null,
                composedAt = timeProvider.currentTime().toString(),
                parentMessageId = null,
                videoLink = null,
                surveyResponse = surveyResponse,
                shouldUpload = true,
                bundleId = null,
                isRead = true,
                surveyId = surveyMessage.surveyId,
                isSurveyComplete = true,
            )
        )
        messagesDao.markSurveyMessageComplete(surveyMessage.id)
    }

    fun getMessageFlow(wallet: String): Flow<List<Message>> {
        return messagesDao.getMessagesForWalletFlow(wallet)
            .map { messages -> messages.map { convertMessageEntityToMessage(it) } }
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

    suspend fun getAnnouncementMessages(id: String): AnnouncementMessage {
        return convertMessageEntityToMessage(messagesDao.getMessage(id)!!) as AnnouncementMessage
    }

    suspend fun getSurveyMessage(id: String): SurveyMessage {
        return convertMessageEntityToMessage(messagesDao.getMessage(id)!!) as SurveyMessage
    }

    /**
     * When uploading trees, messages will be synced locally by this method
     */
    suspend fun syncMessages() = withContext(Dispatchers.IO) {
        for (wallet in userRepo.getUserList().map { it.wallet }) {
            try {
                ensureActive()
                fetchMessagesForWallet(wallet)
            } catch (e: CancellationException) {
              // rethrow cancellation exception
              throw e
            } catch (e: Exception) {
                if (e.localizedMessage == Constants.LOCAL_MSG_ERROR_HTTP404) {
                    // 404 indicates the user has never had messages before
                    continue
                } else {
                    Timber.e(e)
                }
            }
        }

        messageUploader.uploadMessages()
    }

    private suspend fun fetchMessagesForWallet(wallet: String) = withContext(Dispatchers.IO) {
        val lastSyncTime = getLastSyncTime(wallet)
        val query = QueryResponse(
            handle = wallet,
            limit = 100,
            offset = 0,
            total = -1,
        )

        val result = fetchMessagesFromServerAndSaveInDb(query.offset, query.limit, wallet, lastSyncTime)
        if (result.query.total == 0) return@withContext

        var offset = result.query.offset
        val limit = result.query.limit
        val total = result.query.total

        val asyncExecutions = mutableListOf<Deferred<Any>>()

        while (total >= limit + offset) {
            ensureActive()
            offset += limit

            // store this offset value in-case it gets changed in the next iteration.
            val _offset = offset

            asyncExecutions += async {
                fetchMessagesFromServerAndSaveInDb(_offset, limit, wallet, lastSyncTime) // pass _offset instead of offset.
            }
        }

        asyncExecutions.awaitAll()
    }

    private suspend fun fetchMessagesFromServerAndSaveInDb(
        offset: Int,
        limit: Int,
        wallet: String,
        lastSyncTime: String
    ): MessagesResponse = withContext(Dispatchers.IO) {
        val result = apiService.getMessages(
            wallet = wallet,
            lastSyncTime = lastSyncTime,
            offset = offset,
            limit = limit,
        )

        if (result.query.total == 0) return@withContext result

        result.messages.runInParallel {
            saveMessageResponse(wallet, it.copy())
        }

        return@withContext result
    }

    private suspend fun saveMessageResponse(wallet: String, message: MessageResponse): Unit = withContext(Dispatchers.IO) {
        if (message.type == MessageType.SURVEY_RESPONSE) return@withContext

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
                surveyId = survey?.id,
                isSurveyComplete = survey?.let { false },
            )
        }
        messagesDao.insertMessage(messageEntity)

        // If there is no survey, don't continue on
        message.survey ?: return@withContext

        // If survey exists, we'll reuse it
        if (messagesDao.getSurvey(message.survey.id) != null) {
            return@withContext
        }

        val surveyEntity = with(message.survey) {
            SurveyEntity(
                id = id,
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
    }

    private suspend fun convertMessageEntityToMessage(messageEntity: MessageEntity): Message {
        return messagesDao.getSurvey(messageEntity.surveyId)?.let { surveyEntity ->
            val questionEntities = messagesDao.getQuestionsForSurvey(surveyEntity.id)
            DatabaseConverters.createMessageFromEntities(messageEntity, surveyEntity, questionEntities)
        } ?: DatabaseConverters.createMessageFromEntities(messageEntity, null, null)
    }

    private suspend fun getLastSyncTime(wallet: String): String {
        return messagesDao.getLatestSyncTimeForWallet(wallet)
            ?: Instant.fromEpochMilliseconds(0).toString()
    }

    suspend fun checkForUnreadMessages(): Boolean{
        return messagesDao.getUnreadMessagesCount() >= 1
    }

}