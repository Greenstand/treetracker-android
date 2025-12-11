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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO

data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val id: String,
    val isUploaded: Boolean,
    val note: String,
    val plantDate: Instant,
)

data class MapState(
    val markers: List<MapMarker> = emptyList(),
    val isLoading: Boolean = true,
    val selectedMarkerId: String? = null,
)

class MapViewModel(
    private val dao: TreeTrackerDAO
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        loadTrees()
    }

    fun selectMarker(markerId: String) {
        _state.value = _state.value.copy(selectedMarkerId = markerId)
    }

    private fun loadTrees() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val trees = withContext(Dispatchers.IO) {
                // Load both legacy tree captures and new trees
                val allTreeEntities = dao.getAllTrees()
                buildList {
                    addAll(
                        allTreeEntities.map { tree ->
                            MapMarker(
                                latitude = tree.latitude,
                                longitude = tree.longitude,
                                id = "tree_${tree.id}",
                                isUploaded = tree.uploaded,
                                note = tree.note,
                                plantDate = tree.createdAt,
                            )
                        }
                    )
                }
            }

            _state.value = _state.value.copy(
                markers = trees,
                isLoading = false
            )
        }
    }
}
