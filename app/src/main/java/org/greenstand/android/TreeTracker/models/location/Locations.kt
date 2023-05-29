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

enum class Accuracy {
    GOOD,
    BAD,
    NONE
}

data class ConvergenceStats(
    val mean: Double,
    val variance: Double,
    val standardDeviation: Double
)

enum class ConvergenceStatus { CONVERGED, NOT_CONVERGED, TIMED_OUT }

data class LocationData(
    val planterCheckInId: Long?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val treeUuid: String?,
    val convergenceStatus: ConvergenceStatus?,
    val capturedAt: String
)