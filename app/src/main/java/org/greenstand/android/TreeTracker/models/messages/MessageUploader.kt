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

import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.messages.DatabaseConverters
import org.greenstand.android.TreeTracker.database.messages.dao.MessagesDAO
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
            DatabaseConverters.createMessageRequestFromEntities(messageEntity)
        }

        val jsonBundle = gson.toJson(
            UploadBundle.createV2(
                messages = messageRequests,
            )
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