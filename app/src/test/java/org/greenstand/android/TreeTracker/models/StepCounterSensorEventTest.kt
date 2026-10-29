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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Feeds step counter sensor events into a [StepCounter] backed by real preferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StepCounterSensorEventTest {
    private val sensorManager = mockk<SensorManager>(relaxed = true)
    private val listener = slot<SensorEventListener>()
    private lateinit var stepCounter: StepCounter

    @Before
    fun setUp() {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns mockk<Sensor>()
        every {
            sensorManager.registerListener(capture(listener), any<Sensor>(), any<Int>())
        } returns true
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = Preferences(context.getSharedPreferences("step-counter-test", Context.MODE_PRIVATE))
        stepCounter = StepCounter(sensorManager = sensorManager, preferences = preferences)
    }

    @Test
    fun `first step event after enable becomes the baseline`() {
        stepCounter.enable()

        listener.captured.onSensorChanged(sensorEventOf(500f))

        assertEquals(500, stepCounter.absoluteStepCount)
        assertEquals(500, stepCounter.absoluteStepCountOnTreeCapture)
        assertEquals(0, stepCounter.deltaSteps)
    }

    @Test
    fun `later step events count steps since the baseline`() {
        stepCounter.enable()
        listener.captured.onSensorChanged(sensorEventOf(500f))

        listener.captured.onSensorChanged(sensorEventOf(512f))

        assertEquals(512, stepCounter.absoluteStepCount)
        assertEquals(12, stepCounter.deltaSteps)
    }

    @Test
    fun `step count values are truncated to whole steps`() {
        stepCounter.enable()

        listener.captured.onSensorChanged(sensorEventOf(42.9f))

        assertEquals(42, stepCounter.absoluteStepCount)
    }

    @Test
    fun `snapshot on tree capture resets the delta`() {
        stepCounter.enable()
        listener.captured.onSensorChanged(sensorEventOf(500f))
        listener.captured.onSensorChanged(sensorEventOf(530f))

        stepCounter.snapshotAbsoluteStepCountOnTreeCapture()

        assertEquals(530, stepCounter.absoluteStepCountOnTreeCapture)
        assertEquals(0, stepCounter.deltaSteps)
    }

    @Test
    fun `enabling again takes a new baseline from the next event`() {
        stepCounter.enable()
        listener.captured.onSensorChanged(sensorEventOf(100f))
        listener.captured.onSensorChanged(sensorEventOf(150f))

        stepCounter.enable()
        listener.captured.onSensorChanged(sensorEventOf(160f))

        assertEquals(0, stepCounter.deltaSteps)
        listener.captured.onSensorChanged(sensorEventOf(165f))
        assertEquals(5, stepCounter.deltaSteps)
    }

    @Test
    fun `null sensor events are ignored`() {
        stepCounter.enable()
        listener.captured.onSensorChanged(sensorEventOf(500f))

        listener.captured.onSensorChanged(null)

        assertEquals(500, stepCounter.absoluteStepCount)
        assertEquals(0, stepCounter.deltaSteps)
    }

    @Test
    fun `accuracy changes do not touch the step counts`() {
        stepCounter.enable()
        listener.captured.onSensorChanged(sensorEventOf(500f))

        listener.captured.onAccuracyChanged(null, SensorManager.SENSOR_STATUS_ACCURACY_LOW)

        assertEquals(500, stepCounter.absoluteStepCount)
    }
}