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

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeProviderTest {
    private val locationUpdateManager = mockk<LocationUpdateManager>()
    private val timeProvider = TimeProvider(locationUpdateManager)

    @Test
    fun `WHEN a location is known THEN the GPS time is used`() {
        val location = mockk<Location>()
        every { location.time } returns 1_700_000_000_000L
        every { locationUpdateManager.currentLocation } returns location

        assertEquals(Instant.fromEpochMilliseconds(1_700_000_000_000L), timeProvider.currentTime())
    }

    @Test
    fun `WHEN no location is known THEN the system time is used`() {
        every { locationUpdateManager.currentLocation } returns null

        val before = System.currentTimeMillis()
        val time = timeProvider.currentTime().toEpochMilliseconds()
        val after = System.currentTimeMillis()

        assertTrue(time in before..after)
    }

    @Test
    fun `each call reads the latest location`() {
        val first = mockk<Location>()
        every { first.time } returns 1000L
        val second = mockk<Location>()
        every { second.time } returns 2000L
        every { locationUpdateManager.currentLocation } returnsMany listOf(first, second)

        assertEquals(Instant.fromEpochMilliseconds(1000L), timeProvider.currentTime())
        assertEquals(Instant.fromEpochMilliseconds(2000L), timeProvider.currentTime())
    }
}