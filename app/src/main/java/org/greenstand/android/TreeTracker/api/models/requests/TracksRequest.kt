package org.greenstand.android.TreeTracker.api.models.requests

data class TracksRequest(
    val sessionId: String,
    val locations: List<LocationRequest>
)

data class LocationRequest(
    val accuracy: Float,
    val capturedAt: Long,
    val latitude: Double,
    val longitude: Double,
)