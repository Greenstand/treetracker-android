package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import android.location.LocationManager

class GpsUtils(val context: Context) {
    fun hasGPSDevice(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProviders = locationManager.allProviders
        return locationProviders.contains(LocationManager.GPS_PROVIDER)
    }
}