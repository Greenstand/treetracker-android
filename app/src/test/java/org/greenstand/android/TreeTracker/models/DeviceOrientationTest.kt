/*
 * Copyright 2026 Treetracker
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
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceOrientationTest {
    private val sensorManager = mockk<SensorManager>(relaxed = true)
    private val rotationSensor = mockk<Sensor>()
    private lateinit var deviceOrientation: DeviceOrientation

    @Before
    fun setUp() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns rotationSensor
        deviceOrientation = DeviceOrientation(sensorManager)
    }

    @Test
    fun `no snapshot exists before one is taken`() {
        assertNull(deviceOrientation.rotationMatrixSnapshot)
    }

    @Test
    fun `enable registers the orientation listener for the rotation vector sensor`() {
        deviceOrientation.enable()

        verify {
            sensorManager.registerListener(
                deviceOrientation.orientationEventListener,
                rotationSensor,
                SensorManager.SENSOR_DELAY_FASTEST,
            )
        }
    }

    @Test
    fun `enable without a rotation vector sensor registers nothing`() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns null
        val orientationWithoutSensor = DeviceOrientation(sensorManager)

        orientationWithoutSensor.enable()

        verify(exactly = 0) {
            sensorManager.registerListener(any<SensorEventListener>(), any<Sensor>(), any<Int>())
        }
    }

    @Test
    fun `disable unregisters the orientation listener`() {
        deviceOrientation.disable()

        verify { sensorManager.unregisterListener(deviceOrientation.orientationEventListener) }
    }

    @Test
    fun `snapshot before any sensor event is an empty 4x4 matrix`() {
        deviceOrientation.takeSnapshotAndDisable()

        assertArrayEquals(FloatArray(16), deviceOrientation.rotationMatrixSnapshot, 0f)
    }

    @Test
    fun `takeSnapshotAndDisable unregisters the orientation listener`() {
        deviceOrientation.takeSnapshotAndDisable()

        verify { sensorManager.unregisterListener(deviceOrientation.orientationEventListener) }
    }

    @Test
    fun `snapshot holds the identity matrix for a device with no rotation`() {
        deviceOrientation.orientationEventListener.onSensorChanged(sensorEventOf(0f, 0f, 0f, 1f))

        deviceOrientation.takeSnapshotAndDisable()

        val expected =
            floatArrayOf(
                1f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                1f,
            )
        assertArrayEquals(expected, deviceOrientation.rotationMatrixSnapshot, TOLERANCE)
    }

    @Test
    fun `snapshot holds the rotation matrix of the last sensor event`() {
        val halfSqrt2 = 0.70710677f
        deviceOrientation.orientationEventListener.onSensorChanged(sensorEventOf(0f, 0f, 0f, 1f))
        // 90 degree rotation about the z axis
        deviceOrientation.orientationEventListener.onSensorChanged(
            sensorEventOf(0f, 0f, halfSqrt2, halfSqrt2),
        )

        deviceOrientation.takeSnapshotAndDisable()

        val expected =
            floatArrayOf(
                0f,
                -1f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f,
                0f,
                1f,
            )
        assertArrayEquals(expected, deviceOrientation.rotationMatrixSnapshot, TOLERANCE)
    }

    @Test
    fun `null sensor events leave the matrix unchanged`() {
        deviceOrientation.orientationEventListener.onSensorChanged(null)

        deviceOrientation.takeSnapshotAndDisable()

        assertEquals(16, deviceOrientation.rotationMatrixSnapshot?.size)
        assertArrayEquals(FloatArray(16), deviceOrientation.rotationMatrixSnapshot, 0f)
    }

    companion object {
        private const val TOLERANCE = 1e-5f
    }
}