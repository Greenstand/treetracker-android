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
package org.greenstand.android.TreeTracker.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebugOverlayManager {
    private val _showSyncOverlay = MutableStateFlow(false)
    val showSyncOverlay: StateFlow<Boolean> = _showSyncOverlay.asStateFlow()

    private val _showSensorOverlay = MutableStateFlow(false)
    val showSensorOverlay: StateFlow<Boolean> = _showSensorOverlay.asStateFlow()

    private val _showFab = MutableStateFlow(true)
    val showFab: StateFlow<Boolean> = _showFab.asStateFlow()

    fun toggleSyncOverlay() {
        _showSyncOverlay.value = !_showSyncOverlay.value
    }

    fun toggleSensorOverlay() {
        _showSensorOverlay.value = !_showSensorOverlay.value
    }

    fun setSyncOverlay(visible: Boolean) {
        _showSyncOverlay.value = visible
    }

    fun setSensorOverlay(visible: Boolean) {
        _showSensorOverlay.value = visible
    }
}