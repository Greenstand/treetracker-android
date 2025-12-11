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
        radiusMiles: Double = 1.0
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
            centerLon + lonOffset
        )
    }
}