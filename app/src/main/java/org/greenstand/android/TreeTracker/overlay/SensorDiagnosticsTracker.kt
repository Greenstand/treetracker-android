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

import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager

data class GpsState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val convergencePercent: Float = 0f,
    val convergenceStatus: ConvergenceStatus? = null,
    val latStdDev: Double? = null,
    val lonStdDev: Double? = null,
    val latThreshold: Double = 0.00001,
    val lonThreshold: Double = 0.00001,
    val isUpdating: Boolean = false,
)

data class StepState(
    val absoluteCount: Int? = null,
    val deltaSteps: Int = 0,
    val sensorAvailable: Boolean = false,
    val listenerActive: Boolean = false,
)

data class OrientationState(
    val hasRotationSensor: Boolean = false,
    val azimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
)

data class SensorDiagnosticsState(
    val gps: GpsState = GpsState(),
    val steps: StepState = StepState(),
    val orientation: OrientationState = OrientationState(),
)

class SensorDiagnosticsTracker(
    private val locationUpdateManager: LocationUpdateManager,
    private val locationDataCapturer: LocationDataCapturer,
    private val stepCounter: StepCounter,
    private val deviceOrientation: DeviceOrientation,
    private val sensorManager: SensorManager,
) {
    private val _state = MutableStateFlow(SensorDiagnosticsState())
    val state: StateFlow<SensorDiagnosticsState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var pollingJob: Job? = null

    private val convergenceObserver: (Float) -> Unit = { percent ->
        _state.value =
            _state.value.copy(
                gps = _state.value.gps.copy(convergencePercent = percent),
            )
    }

    private val locationObserver =
        Observer<Location?> { location ->
            location?.let {
                _state.value =
                    _state.value.copy(
                        gps =
                            _state.value.gps.copy(
                                latitude = it.latitude,
                                longitude = it.longitude,
                                accuracy = it.accuracy,
                            ),
                    )
            }
        }

    fun start() {
        locationDataCapturer.percentageConvergenceObservers.add(convergenceObserver)
        locationUpdateManager.locationUpdateLiveData.observeForever(locationObserver)

        pollingJob =
            scope.launch {
                while (isActive) {
                    updateState()
                    delay(500)
                }
            }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        locationDataCapturer.percentageConvergenceObservers.remove(convergenceObserver)
        locationUpdateManager.locationUpdateLiveData.removeObserver(locationObserver)
    }

    private fun updateState() {
        val convergence = locationDataCapturer.currentConvergence
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val orientationAngles = FloatArray(3)
        deviceOrientation.rotationMatrixSnapshot?.let { matrix ->
            SensorManager.getOrientation(matrix, orientationAngles)
        }

        _state.value =
            _state.value.copy(
                gps =
                    _state.value.gps.copy(
                        latStdDev = convergence?.latitudeConvergence?.standardDeviation,
                        lonStdDev = convergence?.longitudeConvergence?.standardDeviation,
                        convergenceStatus =
                            if (locationDataCapturer.isConvergenceWithinRange()) {
                                ConvergenceStatus.CONVERGED
                            } else if (locationDataCapturer.generatedTreeUuid != null) {
                                ConvergenceStatus.NOT_CONVERGED
                            } else {
                                null
                            },
                        isUpdating = locationUpdateManager.isUpdating,
                    ),
                steps =
                    StepState(
                        absoluteCount = stepCounter.absoluteStepCount,
                        deltaSteps = stepCounter.deltaSteps,
                        sensorAvailable = stepSensor != null,
                        listenerActive = stepCounter.isListenerRegistered,
                    ),
                orientation =
                    OrientationState(
                        hasRotationSensor = rotationSensor != null,
                        azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat(),
                        pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
                        roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat(),
                    ),
            )
    }
}