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

import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

object LocationUtils {
    /**
     * Generates a random location within a specified radius (in miles) of a center point.
     * Uses uniform distribution within a circle.
     */
    fun generateRandomLocationInRadius(
        centerLat: Double,
        centerLon: Double,
        radiusMiles: Double = 1.0,
    ): Pair<Double, Double> {
        // Convert miles to degrees (approximate)
        // 1 degree of latitude ≈ 69 miles
        val radiusInDegrees = radiusMiles / 69.0

        // Generate random point within a circle using polar coordinates
        val randomAngle = Random.nextDouble() * 2 * Math.PI
        val randomRadius = sqrt(Random.nextDouble()) * radiusInDegrees

        // Calculate offset
        val latOffset = randomRadius * cos(randomAngle)
        val lonOffset = randomRadius * kotlin.math.sin(randomAngle) / cos(Math.toRadians(centerLat))

        return Pair(
            centerLat + latOffset,
            centerLon + lonOffset,
        )
    }
}