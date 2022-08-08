package org.greenstand.android.TreeTracker.utilities

import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager

class TimeProvider(private val locationUpdateManager: LocationUpdateManager) {

    fun currentTime(): Instant {
        val location = locationUpdateManager.currentLocation
        val locationTime = location?.time?.let { Instant.fromEpochMilliseconds(it) }
        return locationTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
    }

}