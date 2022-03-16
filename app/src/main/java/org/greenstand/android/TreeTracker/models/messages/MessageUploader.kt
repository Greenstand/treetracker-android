package org.greenstand.android.TreeTracker.models.messages

import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.models.messages.database.DatabaseConverters
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.utilities.md5
import kotlin.time.ExperimentalTime

class MessageUploader(
    private val objectStorageClient: ObjectStorageClient,
    private val messagesDAO: MessagesDAO,
    private val gson: Gson,
) {

    @OptIn(ExperimentalTime::class)
    suspend fun uploadMessages() {
        coroutineScope {
            messagesDAO.getMessageIdsToUpload()
                .windowed(LIMIT, LIMIT, true)
                .map { async { uploadMessageBundle(it) } }
                .onEach { it.await() }
        }
    }

    private suspend fun uploadMessageBundle(messageIdsToUpload: List<String>) {
        val messageEntitiesToUpload = messagesDAO.getMessagesByIds(messageIdsToUpload)
        val messageRequests = messageEntitiesToUpload.map { messageEntity ->
            messagesDAO.getSurveyForMessage(messageEntity.id)?.let { surveyEntity ->
                DatabaseConverters.createMessageRequestFromEntities(messageEntity, surveyEntity)
            } ?: DatabaseConverters.createMessageRequestFromEntities(messageEntity, null)
        }

        val jsonBundle = gson.toJson(
            UploadBundle.createV2(
                messages = messageRequests,
            ))
        val bundleId = jsonBundle.md5() + "_messages"
        val messageIds = messageEntitiesToUpload.map { it.id }

        // Update the trees in DB with the bundleId
        messagesDAO.updateMessageBundleIds(messageIds, bundleId)
        objectStorageClient.uploadBundle(jsonBundle, bundleId)
        messagesDAO.markMessagesAsUploaded(messageIds)
    }

    companion object {
        private const val LIMIT = 50
    }
}