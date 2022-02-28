package org.greenstand.android.TreeTracker.utilities

import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager

class TimeProvider(private val locationUpdateManager: LocationUpdateManager) {

    fun currentTime(): Long {
        val location = locationUpdateManager.currentLocation
        return location?.time ?: System.currentTimeMillis()
    }

}