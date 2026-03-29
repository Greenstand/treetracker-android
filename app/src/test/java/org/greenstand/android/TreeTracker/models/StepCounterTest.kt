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
import android.hardware.SensorManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class StepCounterTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var sensorManager: SensorManager

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    private lateinit var stepCounter: StepCounter

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        val mockSensor = mockk<Sensor>(relaxed = true)
        every { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) } returns mockSensor

        val mockEditor = mockk<Preferences.Editor>(relaxed = true)
        every { preferences.edit() } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor

        stepCounter =
            StepCounter(
                sensorManager = sensorManager,
                preferences = preferences,
            )
    }

    @Test
    fun `WHEN absoluteStepCount and absoluteStepCountOnTreeCapture set THEN deltaSteps returns difference`() =
        runTest {
            every { preferences.getInt(any(), any()) } returnsMany listOf(100, 80)

            val delta = stepCounter.deltaSteps

            assertEquals(20, delta)
        }

    @Test
    fun `WHEN snapshotAbsoluteStepCountOnTreeCapture called THEN stores current absolute count`() =
        runTest {
            every { preferences.getInt(any(), any()) } returns 150

            stepCounter.snapshotAbsoluteStepCountOnTreeCapture()

            val editor = preferences.edit()
            verify { editor.putInt(any(), 150) }
        }

    @Test
    fun `WHEN enable called THEN registers sensor listener on SensorManager`() =
        runTest {
            stepCounter.enable()

            verify(exactly = 1) {
                sensorManager.registerListener(any<android.hardware.SensorEventListener>(), any<android.hardware.Sensor>(), eq(SensorManager.SENSOR_DELAY_FASTEST))
            }
        }

    @Test
    fun `WHEN disable called THEN unregisters sensor listener`() =
        runTest {
            stepCounter.disable()

            verify(exactly = 1) {
                sensorManager.unregisterListener(any<android.hardware.SensorEventListener>())
            }
        }
}