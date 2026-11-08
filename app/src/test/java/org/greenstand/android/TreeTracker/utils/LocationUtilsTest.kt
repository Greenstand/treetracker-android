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
package org.greenstand.android.TreeTracker.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

class LocationUtilsTest {
    @Test
    fun `zero radius returns the center`() {
        val (lat, lon) = LocationUtils.generateRandomLocationInRadius(37.42, -122.08, radiusMiles = 0.0)

        assertEquals(37.42, lat, 0.0)
        assertEquals(-122.08, lon, 0.0)
    }

    @Test
    fun `random locations stay within the radius`() {
        val centerLat = 37.42
        val centerLon = -122.08
        val radiusMiles = 2.0
        val radiusDegrees = radiusMiles / MILES_PER_DEGREE

        repeat(500) {
            val (lat, lon) = LocationUtils.generateRandomLocationInRadius(centerLat, centerLon, radiusMiles)

            // Undo the longitude stretch to measure the distance in latitude degrees
            val latOffset = lat - centerLat
            val lonOffset = (lon - centerLon) * cos(Math.toRadians(centerLat))
            val distance = sqrt(latOffset.pow(2) + lonOffset.pow(2))
            assertTrue("distance $distance exceeds $radiusDegrees", distance <= radiusDegrees + EPSILON)
        }
    }

    @Test
    fun `default radius is one mile`() {
        repeat(200) {
            val (lat, lon) = LocationUtils.generateRandomLocationInRadius(0.0, 0.0)

            val distance = sqrt(lat.pow(2) + lon.pow(2))
            assertTrue(distance <= 1.0 / MILES_PER_DEGREE + EPSILON)
        }
    }

    companion object {
        private const val MILES_PER_DEGREE = 69.0
        private const val EPSILON = 1e-12
    }
}