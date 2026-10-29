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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConvergenceConfigurationTest {
    private val configurator = mockk<Configurator>()

    @Before
    fun setUp() {
        stubConfig(timeout = 30000, dataSize = 8, lonThreshold = 0.0002f, latThreshold = 0.0003f)
    }

    private fun stubConfig(
        timeout: Int,
        dataSize: Int,
        lonThreshold: Float,
        latThreshold: Float,
    ) {
        every { configurator.getInt(ConfigKeys.CONVERGENCE_TIMEOUT) } returns timeout
        every { configurator.getInt(ConfigKeys.CONVERGENCE_DATA_SIZE) } returns dataSize
        every { configurator.getFloat(ConfigKeys.LON_STD_DEV_THRESHOLD) } returns lonThreshold
        every { configurator.getFloat(ConfigKeys.LAT_STD_DEV_THRESHOLD) } returns latThreshold
    }

    @Test
    fun `config is read from the configurator on creation`() {
        val configuration = ConvergenceConfiguration(configurator)

        val config = configuration.locationDataConfig
        assertEquals(30000L, config.convergenceTimeout)
        assertEquals(8, config.convergenceDataSize)
        assertEquals(0.0002f, config.lonStdDevThreshold, 0f)
        assertEquals(0.0003f, config.latStdDevThreshold, 0f)
    }

    @Test
    fun `location update intervals keep their defaults`() {
        val configuration = ConvergenceConfiguration(configurator)

        assertEquals(1000L, configuration.locationDataConfig.minTimeBetweenUpdates)
        assertEquals(0f, configuration.locationDataConfig.minDistanceBetweenUpdates, 0f)
    }

    @Test
    fun `refreshConfig picks up changed values`() {
        val configuration = ConvergenceConfiguration(configurator)
        stubConfig(timeout = 5000, dataSize = 3, lonThreshold = 0.1f, latThreshold = 0.2f)

        configuration.refreshConfig()

        assertEquals(
            LocationDataConfig(
                convergenceTimeout = 5000L,
                convergenceDataSize = 3,
                lonStdDevThreshold = 0.1f,
                latStdDevThreshold = 0.2f,
            ),
            configuration.locationDataConfig,
        )
    }

    @Test
    fun `config is not re-read until refreshConfig is called`() {
        val configuration = ConvergenceConfiguration(configurator)
        stubConfig(timeout = 5000, dataSize = 3, lonThreshold = 0.1f, latThreshold = 0.2f)

        assertEquals(30000L, configuration.locationDataConfig.convergenceTimeout)
        verify(exactly = 1) { configurator.getInt(ConfigKeys.CONVERGENCE_TIMEOUT) }
    }

    @Test
    fun `LocationDataConfig defaults match the documented convergence parameters`() {
        val config = LocationDataConfig()

        assertEquals(1000L, config.minTimeBetweenUpdates)
        assertEquals(0f, config.minDistanceBetweenUpdates, 0f)
        assertEquals(20000L, config.convergenceTimeout)
        assertEquals(5, config.convergenceDataSize)
        assertEquals(0.00001f, config.lonStdDevThreshold, 0f)
        assertEquals(0.00001f, config.latStdDevThreshold, 0f)
    }
}