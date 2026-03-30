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
package org.greenstand.android.TreeTracker.map

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import timber.log.Timber

data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val id: String,
    val isUploaded: Boolean,
    val note: String,
    val plantDate: Instant,
    val imagePath: String?,
    val sessionId: Long? = null,
)

data class StepPoint(
    val latitude: Double,
    val longitude: Double,
    val sessionId: Long,
)

data class MapState(
    val markers: List<MapMarker> = emptyList(),
    val stepPoints: List<StepPoint> = emptyList(),
    val isLoading: Boolean = true,
    val selectedMarkerId: String? = null,
    val selectedSessionId: Long? = null,
)

sealed class MapAction : Action {
    data class SelectMarker(
        val markerId: String,
    ) : MapAction()
}

class MapViewModel(
    private val dao: TreeTrackerDAO,
) : BaseViewModel<MapState, MapAction>(MapState()) {
    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadData()
    }

    override fun handleAction(action: MapAction) {
        when (action) {
            is MapAction.SelectMarker -> {
                val marker = state.value.markers.find { it.id == action.markerId }
                updateState {
                    copy(
                        selectedMarkerId = action.markerId,
                        selectedSessionId = marker?.sessionId,
                    )
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            val (trees, steps) =
                withContext(Dispatchers.IO) {
                    val allTreeEntities = dao.getAllTrees()
                    val treeMarkers =
                        allTreeEntities.map { tree ->
                            MapMarker(
                                latitude = tree.latitude,
                                longitude = tree.longitude,
                                id = "tree_${tree.id}",
                                isUploaded = tree.uploaded,
                                note = tree.note,
                                plantDate = tree.createdAt,
                                imagePath = tree.photoPath,
                                sessionId = tree.sessionId,
                            )
                        }

                    val allLocations = dao.getAllLocations()
                    val stepPoints =
                        allLocations.mapNotNull { entity ->
                            try {
                                val data = json.decodeFromString<LocationData>(entity.locationDataJson)
                                StepPoint(
                                    latitude = data.latitude,
                                    longitude = data.longitude,
                                    sessionId = entity.sessionId,
                                )
                            } catch (e: Exception) {
                                Timber.w(e, "Failed to parse location JSON")
                                null
                            }
                        }

                    Pair(treeMarkers, stepPoints)
                }

            updateState {
                copy(markers = trees, stepPoints = steps, isLoading = false)
            }
        }
    }
}
