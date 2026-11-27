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
package org.greenstand.android.TreeTracker.models.location

import android.location.Location
import org.greenstand.android.TreeTracker.models.ConvergenceStats
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

class Convergence(
    val locations: List<Location>,
) {
    var longitudeConvergence: ConvergenceStats? = null
        private set
    var latitudeConvergence: ConvergenceStats? = null
        private set

    fun computeSlidingWindowStats(
        currentStats: ConvergenceStats,
        replacingValue: Double,
        newValue: Double,
    ): ConvergenceStats {
        val newMean =
            currentStats.mean -
                (replacingValue / locations.size) +
                (newValue / locations.size)
        val newVariance =
            currentStats.variance -
                ((replacingValue - currentStats.mean).pow(2.0) / locations.size) +
                ((newValue - newMean).pow(2.0) / locations.size)
        val newStdDev = sqrt(newVariance)
        return ConvergenceStats(newMean, newVariance, newStdDev)
    }

    fun computeConvergence() {
        Timber.d("Convergence: Evaluating initial convergence stats")
        val longitudeData = locations.map { it.longitude }.toList()
        longitudeConvergence = computeStats(longitudeData)

        val latitudeData = locations.map { it.latitude }.toList()
        latitudeConvergence = computeStats(latitudeData)
    }

    fun computeSlidingWindowConvergence(
        replaceLocation: Location,
        newLocation: Location,
    ) {
        Timber.d("Convergence: Evaluating running convergence stats")
        longitudeConvergence =
            computeSlidingWindowStats(
                longitudeConvergence!!,
                replaceLocation.longitude,
                newLocation.longitude,
            )
        latitudeConvergence =
            computeSlidingWindowStats(
                latitudeConvergence!!,
                replaceLocation.latitude,
                newLocation.latitude,
            )
    }

    fun longitudinalStandardDeviation(): Double? = longitudeConvergence?.standardDeviation

    fun latitudinalStandardDeviation(): Double? = latitudeConvergence?.standardDeviation

    private fun computeStats(data: List<Double>): ConvergenceStats {
        val mean = data.sum() / data.size
        var variance = 0.0
        for (x in data) {
            variance += (x - mean).pow(2.0)
        }
        variance /= data.size
        val stdDev = sqrt(variance)
        return ConvergenceStats(mean, variance, stdDev)
    }
}