package org.greenstand.android.TreeTracker.models.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenstand.android.TreeTracker.models.Configuration
import timber.log.Timber

class LocationUpdateManager(
    private val locationManager: LocationManager,
    private val context: Context,
    private val configuration: Configuration
) {

    private val locationUpdates = MutableLiveData<Location?>()
    val locationUpdateLiveData: LiveData<Location?> = locationUpdates

    var isUpdating: Boolean = false
        private set

    var currentLocation: Location? = null
        private set

    init {
        locationUpdates.postValue(null)
    }

    private val locationUpdateListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            Timber.d("Posting location value $currentLocation")
            locationUpdates.postValue(location)
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            Timber.d("Location status changed %s %d", p0, p1)
        }
    }

    fun startLocationUpdates(): Boolean {
        return if (hasLocationPermissions()) {
            if (!isUpdating) {
                val locationDataConfig = configuration.locationDataConfig
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    locationDataConfig.minTimeBetweenUpdates,
                    locationDataConfig.minDistanceBetweenUpdates,
                    locationUpdateListener
                )
                isUpdating = true
            }
            true
        } else {
            isUpdating = false
            false
        }
    }

    fun stopLocationUpdates(): Boolean {
        Timber.d("Request to stop location updates submitted")
        if (locationUpdateLiveData.hasObservers())
            return false
        Timber.d("Request to stop location update honored")
        locationManager.removeUpdates(locationUpdateListener)
        locationUpdates.postValue(null)
        isUpdating = false
        return true
    }

    /**
     *  Meant to be used to dynamically update the location update request with
     *  updated values for min time between updates and min distance between updates.
     */
    fun refreshLocationUpdateRequest() {
        val locationDataConfig = configuration.locationDataConfig
        locationManager.removeUpdates(locationUpdateListener)
        if (hasLocationPermissions()) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                locationDataConfig.minTimeBetweenUpdates,
                locationDataConfig.minDistanceBetweenUpdates,
                locationUpdateListener
            )
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return (
                fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                        coarseLocationPermission == PackageManager.PERMISSION_GRANTED
                )
    }
}