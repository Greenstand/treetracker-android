package org.greenstand.android.TreeTracker.utilities

import android.content.Context
import android.location.LocationManager
import androidx.activity.ComponentActivity

class GpsUtils(val context: Context) {
    fun hasGPSDevice(): Boolean {
        val mgr = context.getSystemService(ComponentActivity.LOCATION_SERVICE) as LocationManager
        val providers = mgr.allProviders
        return providers.contains(LocationManager.GPS_PROVIDER)
    }
}