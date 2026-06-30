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
package org.greenstand.android.TreeTracker.usecases

import com.amazonaws.AmazonClientException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.LocationRequest
import org.greenstand.android.TreeTracker.api.models.requests.TracksRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber

class UploadLocationDataUseCase(
    private val dao: TreeTrackerDAO,
    private val json: Json,
) : UseCase<Unit, Boolean>() {
    private val storageClient = ObjectStorageClient.instance()

    override suspend fun execute(params: Unit): Boolean {
        try {
            Timber.d("Processing tree location data")
            withContext(Dispatchers.IO) {
                // V2
                val locationEntities = dao.getLocationData()
                val sessionIdToLocations = locationEntities.groupBy { it.sessionId }
                val sessionIdToLocationRequests =
                    sessionIdToLocations
                        .map { (sessionId, entities) ->
                            val locationRequests =
                                entities
                                    .mapNotNull {
                                        try {
                                            json.decodeFromString<LocationData>(it.locationDataJson)
                                        } catch (e: Exception) {
                                            Timber.e(e, "Failed to decode location data for entity ${it.id}")
                                            null
                                        }
                                    }.map {
                                        LocationRequest(
                                            accuracy = it.accuracy,
                                            latitude = it.latitude,
                                            longitude = it.longitude,
                                            capturedAt = it.capturedAt,
                                        )
                                    }
                            return@map sessionId to locationRequests
                        }

                val trackRequests =
                    sessionIdToLocationRequests.mapNotNull { (sessionId, locationList) ->
                        val sessionEntity = dao.getSessionById(sessionId)
                        if (sessionEntity == null) {
                            Timber.w("Skipping location upload for session $sessionId: session not found in DB")
                            return@mapNotNull null
                        }
                        TracksRequest(
                            sessionId = sessionEntity.uuid,
                            locations = locationList,
                        )
                    }

                if (trackRequests.isEmpty()) {
                    Timber.d("No valid track requests to upload")
                    return@withContext
                }

                val dataBundle =
                    json.encodeToString(
                        UploadBundle.createV2(
                            tracks = trackRequests,
                        ),
                    )
                storageClient.uploadBundle(
                    dataBundle,
                    "${dataBundle.md5()}_tracks",
                )

                dao.updateLocationDataUploadStatus(locationEntities.map { it.id }, true)
                dao.purgeUploadedLocations()

                Timber.tag("Location Upload").d("Completed uploading ${locationEntities.size} V2 GPS locations")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (ace: AmazonClientException) {
            Timber.e(ace, "AmazonClientException encountered while communicating with S3")
            return false
        } catch (e: Exception) {
            Timber.e(e, "Location upload error")
            return false
        }
        return true
    }
}