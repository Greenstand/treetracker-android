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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.api.models.requests.LocationRequest
import org.greenstand.android.TreeTracker.api.models.requests.TracksRequest
import org.greenstand.android.TreeTracker.api.models.requests.UploadBundle
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber
import java.io.IOException

class UploadLocationDataUseCase(
    private val dao: TreeTrackerDAO,
    private val json: Json,
    private val exceptionDataCollector: ExceptionDataCollector,
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
                                    .map { json.decodeFromString<LocationData>(it.locationDataJson) }
                                    .map {
                                        LocationRequest(
                                            accuracy = it.accuracy,
                                            latitude = it.latitude,
                                            longitude = it.longitude,
                                            capturedAt = it.capturedAt,
                                        )
                                    }
                            return@map sessionId to locationRequests
                        }

                val sessionEntities = sessionIdToLocations.map { dao.getSessionById(it.key) }
                val trackRequests =
                    sessionIdToLocationRequests.map { (sessionId, locationList) ->
                        TracksRequest(
                            sessionId = sessionEntities.find { it.id == sessionId }!!.uuid,
                            locations = locationList,
                        )
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
        } catch (e: SerializationException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_PARSING, e, "Serialization failure during location data upload")
            return false
        } catch (ace: AmazonClientException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_SERVER, ace, "Storage server failure during location data upload")
            return false
        } catch (e: IOException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_NETWORK, e, "Network failure during location data upload")
            return false
        } catch (e: Exception) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_UNKNOWN, e, "Unexpected location upload error")
            return false
        }
        return true
    }
}