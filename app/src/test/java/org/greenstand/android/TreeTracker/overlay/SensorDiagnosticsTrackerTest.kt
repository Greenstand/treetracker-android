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
package org.greenstand.android.TreeTracker.overlay

import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.ConvergenceStats
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.Convergence
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SensorDiagnosticsTrackerTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val locationLiveData = MutableLiveData<Location?>()
    private val convergenceObservers = mutableListOf<(Float) -> Unit>()
    private val locationUpdateManager = mockk<LocationUpdateManager>()
    private val locationDataCapturer = mockk<LocationDataCapturer>()
    private val stepCounter = mockk<StepCounter>()
    private val deviceOrientation = mockk<DeviceOrientation>()
    private val sensorManager = mockk<SensorManager>()
    private lateinit var tracker: SensorDiagnosticsTracker

    @Before
    fun setUp() {
        every { locationUpdateManager.locationUpdateLiveData } returns locationLiveData
        every { locationUpdateManager.isUpdating } returns true
        every { locationDataCapturer.percentageConvergenceObservers } returns convergenceObservers
        every { locationDataCapturer.currentConvergence } returns null
        every { locationDataCapturer.isConvergenceWithinRange() } returns false
        every { locationDataCapturer.generatedTreeUuid } returns null
        every { stepCounter.absoluteStepCount } returns 1200
        every { stepCounter.deltaSteps } returns 15
        every { stepCounter.isListenerRegistered } returns true
        every { deviceOrientation.rotationMatrixSnapshot } returns null
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns mockk()
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns mockk()
        tracker =
            SensorDiagnosticsTracker(
                locationUpdateManager = locationUpdateManager,
                locationDataCapturer = locationDataCapturer,
                stepCounter = stepCounter,
                deviceOrientation = deviceOrientation,
                sensorManager = sensorManager,
            )
    }

    @After
    fun tearDown() {
        tracker.stop()
    }

    private fun advancePolling() {
        val scheduler = mainCoroutineRule.testDispatcher.scheduler
        scheduler.advanceTimeBy(POLL_INTERVAL_MS)
        scheduler.runCurrent()
    }

    @Test
    fun `state is empty before start`() {
        assertEquals(SensorDiagnosticsState(), tracker.state.value)
    }

    @Test
    fun `start reads the step counter and sensor availability`() {
        tracker.start()

        val state = tracker.state.value
        assertEquals(
            StepState(absoluteCount = 1200, deltaSteps = 15, sensorAvailable = true, listenerActive = true),
            state.steps,
        )
        assertTrue(state.orientation.hasRotationSensor)
        assertTrue(state.gps.isUpdating)
    }

    @Test
    fun `missing sensors are reported as unavailable`() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns null
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns null
        every { stepCounter.absoluteStepCount } returns null
        every { stepCounter.isListenerRegistered } returns false

        tracker.start()

        val state = tracker.state.value
        assertEquals(
            StepState(absoluteCount = null, deltaSteps = 15, sensorAvailable = false, listenerActive = false),
            state.steps,
        )
        assertFalse(state.orientation.hasRotationSensor)
    }

    @Test
    fun `convergence status is null outside of tree capture`() {
        tracker.start()

        assertNull(tracker.state.value.gps.convergenceStatus)
    }

    @Test
    fun `convergence status is not converged during tree capture`() {
        every { locationDataCapturer.generatedTreeUuid } returns UUID.randomUUID()

        tracker.start()

        assertEquals(ConvergenceStatus.NOT_CONVERGED, tracker.state.value.gps.convergenceStatus)
    }

    @Test
    fun `convergence status is converged when within range`() {
        every { locationDataCapturer.isConvergenceWithinRange() } returns true
        every { locationDataCapturer.generatedTreeUuid } returns UUID.randomUUID()

        tracker.start()

        assertEquals(ConvergenceStatus.CONVERGED, tracker.state.value.gps.convergenceStatus)
    }

    @Test
    fun `standard deviations come from the current convergence`() {
        val convergence = mockk<Convergence>()
        every { convergence.latitudeConvergence } returns ConvergenceStats(1.0, 0.04, 0.2)
        every { convergence.longitudeConvergence } returns ConvergenceStats(2.0, 0.09, 0.3)
        every { locationDataCapturer.currentConvergence } returns convergence

        tracker.start()

        val gps = tracker.state.value.gps
        assertEquals(0.2, gps.latStdDev!!, 0.0)
        assertEquals(0.3, gps.lonStdDev!!, 0.0)
    }

    @Test
    fun `without a convergence the standard deviations are null`() {
        tracker.start()

        assertNull(tracker.state.value.gps.latStdDev)
        assertNull(tracker.state.value.gps.lonStdDev)
    }

    @Test
    fun `without a rotation snapshot the orientation angles are zero`() {
        tracker.start()

        val orientation = tracker.state.value.orientation
        assertEquals(0f, orientation.azimuth, 0f)
        assertEquals(0f, orientation.pitch, 0f)
        assertEquals(0f, orientation.roll, 0f)
    }

    @Test
    fun `orientation angles are derived from the rotation snapshot in degrees`() {
        // azimuth = atan2(R[1], R[5]), pitch = asin(-R[9]), roll = atan2(-R[8], R[10])
        val matrix = FloatArray(16)
        matrix[1] = 1f
        matrix[5] = 0f
        matrix[9] = -0.5f
        matrix[10] = 1f
        every { deviceOrientation.rotationMatrixSnapshot } returns matrix

        tracker.start()

        val orientation = tracker.state.value.orientation
        assertEquals(90f, orientation.azimuth, ANGLE_TOLERANCE)
        assertEquals(30f, orientation.pitch, ANGLE_TOLERANCE)
        assertEquals(0f, orientation.roll, ANGLE_TOLERANCE)
    }

    @Test
    fun `location updates are shown in the gps state`() {
        tracker.start()
        val location =
            Location(LocationManager.GPS_PROVIDER).apply {
                latitude = 37.42
                longitude = -122.08
                accuracy = 4f
            }

        locationLiveData.value = location

        val gps = tracker.state.value.gps
        assertEquals(37.42, gps.latitude!!, 0.0)
        assertEquals(-122.08, gps.longitude!!, 0.0)
        assertEquals(4f, gps.accuracy!!, 0f)
    }

    @Test
    fun `null location updates are ignored`() {
        tracker.start()

        locationLiveData.value = null

        assertNull(tracker.state.value.gps.latitude)
        assertNull(tracker.state.value.gps.longitude)
    }

    @Test
    fun `location is kept when the sensors are polled again`() {
        tracker.start()
        locationLiveData.value =
            Location(LocationManager.GPS_PROVIDER).apply {
                latitude = 1.0
                longitude = 2.0
            }

        advancePolling()

        assertEquals(1.0, tracker.state.value.gps.latitude!!, 0.0)
    }

    @Test
    fun `convergence percentage updates are shown in the gps state`() {
        tracker.start()

        assertEquals(1, convergenceObservers.size)
        convergenceObservers.single().invoke(0.4f)

        assertEquals(0.4f, tracker.state.value.gps.convergencePercent, 0f)
    }

    @Test
    fun `sensors are polled again every half second`() {
        tracker.start()
        every { stepCounter.deltaSteps } returns 20

        assertEquals(15, tracker.state.value.steps.deltaSteps)
        advancePolling()

        assertEquals(20, tracker.state.value.steps.deltaSteps)
    }

    @Test
    fun `stop removes the observers and stops polling`() {
        tracker.start()

        tracker.stop()
        every { stepCounter.deltaSteps } returns 20
        advancePolling()

        assertTrue(convergenceObservers.isEmpty())
        assertFalse(locationLiveData.hasObservers())
        assertEquals(15, tracker.state.value.steps.deltaSteps)
    }

    @Test
    fun `tracker can be started again after stop`() {
        tracker.start()
        tracker.stop()
        every { stepCounter.deltaSteps } returns 20

        tracker.start()

        assertEquals(1, convergenceObservers.size)
        assertTrue(locationLiveData.hasObservers())
        assertEquals(20, tracker.state.value.steps.deltaSteps)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val ANGLE_TOLERANCE = 0.01f
    }
}