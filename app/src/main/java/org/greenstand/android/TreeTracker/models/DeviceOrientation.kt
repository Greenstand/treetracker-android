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

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber

class DeviceOrientation(
    private val sensorManager: SensorManager
) : LifecycleObserver {

    val orientationEventListener = OrientationEventListener()
    var rotationMatrixSnapshot: FloatArray? = null
        private set
    private var rotationMatrix: FloatArray = FloatArray(16)
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun enable() {
        Timber.d("DeviceOrientation - registering rotation vector sensor")
        sensorManager.registerListener(
            orientationEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun disable() {
        Timber.d("DeviceOrientation - unregistering rotation vector sensor")
        sensorManager.unregisterListener(orientationEventListener)
    }

    fun takeSnapshotAndDisable() {
        Timber.d("DeviceOrientation - snapshot rotation matrix")
        rotationMatrixSnapshot = rotationMatrix
        disable()
    }

    inner class OrientationEventListener : SensorEventListener {

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Ignore
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, it.values)
                Timber.d("DeviceOrientation - Rotation Matrix " +
                        "[${rotationMatrix.joinToString(",")}]")
            }
        }
    }
}
