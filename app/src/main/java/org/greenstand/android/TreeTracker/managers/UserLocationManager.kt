package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber

class UserLocationManager(private val locationManager: LocationManager,
                          private val context: Context) {

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private val locationUpdates = BroadcastChannel<Location>(1)

    @UseExperimental(ExperimentalCoroutinesApi::class)
    val locationUpdatesChannel: ReceiveChannel<Location> = locationUpdates.openSubscription()

    var isUpdating: Boolean = false
        private set

    var currentLocation: Location? = null
        private set

    private val locationUpdateListener = object : android.location.LocationListener  {

        @UseExperimental(ExperimentalCoroutinesApi::class)
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            locationUpdates.offer(location)
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            Timber.d("Location status changed %s %d", p0, p1)

        }

        override fun onProviderEnabled(p0: String?) {
            Timber.d("Provider enabled %s", p0)
        }

        override fun onProviderDisabled(p0: String?) {
            Timber.d("Provider disabled %s", p0)
        }
    }

    fun startLocationUpdates(): Boolean {
        return if (hasLocationPermissions()) {
            if (!isUpdating) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationUpdateListener)
                isUpdating = true
            }
            true
        } else {
            isUpdating = false
            false
        }
    }

    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationUpdateListener)
        isUpdating = false
    }

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun hasLocationPermissions() : Boolean {
        return ContextCompat.checkSelfPermission(context,
                                                 android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context,
                                                     android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}