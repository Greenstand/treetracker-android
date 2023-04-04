package org.greenstand.android.TreeTracker.api.models.requests

data class TracksRequest(
    val sessionId: String,
    val locations: List<List<Any>>
)