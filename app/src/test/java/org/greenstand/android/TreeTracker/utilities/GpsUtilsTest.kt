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
package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import android.location.LocationManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsUtilsTest {
    private val locationManager = mockk<LocationManager>()
    private val context =
        mockk<Context> {
            every { getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        }

    @Test
    fun `device with a gps provider has gps`() {
        every { locationManager.allProviders } returns
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)

        assertTrue(GpsUtils(context).hasGPSDevice())
    }

    @Test
    fun `device without a gps provider has no gps`() {
        every { locationManager.allProviders } returns listOf(LocationManager.NETWORK_PROVIDER)

        assertFalse(GpsUtils(context).hasGPSDevice())
    }

    @Test
    fun `device without any providers has no gps`() {
        every { locationManager.allProviders } returns emptyList()

        assertFalse(GpsUtils(context).hasGPSDevice())
    }
}