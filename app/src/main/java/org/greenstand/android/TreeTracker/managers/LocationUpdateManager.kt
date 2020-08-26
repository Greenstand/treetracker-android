package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.util.*


class LocationUpdateManager(
    private val locationManager: LocationManager,
    private val context: Context
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
        if (locationUpdateLiveData.hasObservers())
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

        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return (fineLocationPermission == PackageManager.PERMISSION_GRANTED ||
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED)
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

class LocationDataCapturer(
    private val userManager: UserManager,
    private val locationUpdateManager: LocationUpdateManager,
    private val preferences: Preferences,
    private val treeTrackerDAO: TreeTrackerDAO
) {

    private val gson = Gson()
    var generatedTreeUuid: UUID? = null
        private set

    private val locationObserver: Observer<Location?> = Observer {
        it?.apply {
            MainScope().launch(Dispatchers.IO) {

                val locationData =
                    LocationData(
                        userManager.planterCheckinId,
                        latitude,
                        longitude,
                        accuracy,
                        generatedTreeUuid?.toString() ?: null,
                        System.currentTimeMillis()
                    )
                Timber.d("Generated Location Data value ${locationData}")
//                val base64String = Base64.encodeToString(
//                    gson.toJson(locationData).toByteArray(),
//                    Base64.NO_WRAP
//                )
//                Timber.d("Inserting a new location data $base64String")

                val jsonString = Gson().toJson(locationData)
                treeTrackerDAO.insertLocationData(LocationDataEntity(jsonString))
            }
        }
    }

    fun start() {
        val livedata = locationUpdateManager.locationUpdateLiveData
        livedata.observeForever(locationObserver)
    }

    fun turnOnTreeCaptureMode() {
        generatedTreeUuid = UUID.randomUUID()
        Timber.d("Tree capture mode turned on")
    }

    fun turnOffTreeCaptureMode() {
        generatedTreeUuid = null
        Timber.d("Tree capture turned off")
    }
}

data class LocationData(
    val planterCheckInId: Long?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val treeUuid: String?,
    val capturedAt: Long
)
