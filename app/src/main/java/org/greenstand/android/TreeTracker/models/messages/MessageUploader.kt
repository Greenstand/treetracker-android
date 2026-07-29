/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models.messages

import com.amazonaws.AmazonClientException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.models.messages.database.DatabaseConverters
import org.greenstand.android.TreeTracker.models.messages.database.MessagesDAO
import org.greenstand.android.TreeTracker.utilities.md5
import java.io.IOException
import kotlin.time.ExperimentalTime

class MessageUploader(
    private val objectStorageClient: ObjectStorageClient,
    private val messagesDAO: MessagesDAO,
    private val json: Json,
    private val exceptionDataCollector: ExceptionDataCollector,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun uploadMessages() {
        try {
            coroutineScope {
                messagesDAO
                    .getMessageIdsToUpload()
                    .windowed(LIMIT, LIMIT, true)
                    .map { async { uploadMessageBundle(it) } }
                    .onEach { it.await() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_PARSING, e, "Serialization failure during message upload")
            throw e
        } catch (e: Exception) {
            val (failureType, message) =
                when (e) {
                    is IOException -> ExceptionDataCollector.TYPE_NETWORK to "Network failure during message upload"
                    is AmazonClientException -> ExceptionDataCollector.TYPE_SERVER to "Storage server failure during message upload"
                    else -> ExceptionDataCollector.TYPE_UNKNOWN to "Unexpected failure during message upload"
                }
            exceptionDataCollector.recordFailure(failureType, e, message)
            throw e
        }
    }

    private suspend fun uploadMessageBundle(messageIdsToUpload: List<String>) {
        val messageEntitiesToUpload = messagesDAO.getMessagesByIds(messageIdsToUpload)
        val messageRequests =
            messageEntitiesToUpload.map { messageEntity ->
                DatabaseConverters.createMessageRequestFromEntities(messageEntity)
            }

        val jsonBundle =
            json.encodeToString(
                UploadBundle.createV2(
                    messages = messageRequests,
                ),
            )
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