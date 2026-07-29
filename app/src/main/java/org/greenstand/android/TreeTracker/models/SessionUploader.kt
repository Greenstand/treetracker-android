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
package org.greenstand.android.TreeTracker.models

import com.amazonaws.AmazonClientException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.SessionRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5
import java.io.IOException

class SessionUploader(
    private val dao: TreeTrackerDAO,
    private val objectStorageClient: ObjectStorageClient,
    private val json: Json,
    private val exceptionDataCollector: ExceptionDataCollector,
) {
    suspend fun upload() {
        try {
            val sessionsToUpload = dao.getSessionsToUpload()

            val sessionRequests =
                sessionsToUpload.map { session ->
                    SessionRequest(
                        sessionId = session.uuid,
                        originUserId = session.originUserId,
                        targetWallet = session.destinationWallet,
                        organization = session.organization ?: "",
                        deviceConfigId = dao.getDeviceConfigById(session.deviceConfigId!!)!!.uuid,
                    )
                }

            val jsonBundle =
                json.encodeToString(
                    UploadBundle.createV2(
                        sessions = sessionRequests,
                    ),
                )
            val bundleId = jsonBundle.md5() + "_sessions"
            val sessionIds = sessionsToUpload.map { it.id }

            // Update the trees in DB with the bundleId
            dao.updateSessionBundleIds(sessionIds, bundleId)
            objectStorageClient.uploadBundle(jsonBundle, bundleId)
            dao.updateSessionUploadStatus(sessionIds, true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_PARSING, e, "Serialization failure during session upload")
            throw e
        } catch (e: Exception) {
            val (failureType, message) =
                when (e) {
                    is IOException -> ExceptionDataCollector.TYPE_NETWORK to "Network failure during session upload"
                    is AmazonClientException -> ExceptionDataCollector.TYPE_SERVER to "Storage server failure during session upload"
                    else -> ExceptionDataCollector.TYPE_UNKNOWN to "Unexpected failure during session upload"
                }
            exceptionDataCollector.recordFailure(failureType, e, message)
            throw e
        }
    }
}