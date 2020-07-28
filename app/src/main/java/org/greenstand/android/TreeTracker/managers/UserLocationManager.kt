package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

class UserLocationManager(
    private val locationManager: LocationManager,
    private val context: Context
) {

    private val locationUpdates = MutableLiveData<Location?>()
    val locationUpdateLiveDate: LiveData<Location?> = locationUpdates

    var isUpdating: Boolean = false
        private set

    var currentLocation: Location? = null
        private set

    init {
        locationUpdates.postValue(null)
    }

    private val locationUpdateListener = object : android.location.LocationListener {

        @UseExperimental(ExperimentalCoroutinesApi::class)
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            locationUpdates.postValue(location)
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
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0f,
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

    fun stopLocationUpdates() {
        Timber.d("Request to stop location updates submitted")
        if (locationUpdateLiveDate.hasObservers())
            return
        Timber.d("Request to stop location update honored")
        locationManager.removeUpdates(locationUpdateListener)
        locationUpdates.postValue(null)
        isUpdating = false
    }

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun hasSufficientAccuracy(): Boolean {
        return currentLocation?.let {
            it.hasAccuracy() && it.accuracy < ValueHelper.MIN_ACCURACY_DEFAULT_SETTING
        } ?: false
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}

enum class Accuracy {
    GOOD,
    BAD,
    NONE
}

fun Location?.accuracyStatus(): Accuracy {
    if (this == null || !hasAccuracy()) {
        return Accuracy.NONE
    }
    return if (accuracy < ValueHelper.MIN_ACCURACY_DEFAULT_SETTING) {
        Accuracy.GOOD
    } else {
        Accuracy.BAD
    }
}
