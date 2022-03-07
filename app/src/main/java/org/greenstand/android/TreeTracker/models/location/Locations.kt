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
