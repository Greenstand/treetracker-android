package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.gson.GsonBuilder
import java.util.UUID
import java.util.Deque
import java.util.LinkedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

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
    private val treeTrackerDAO: TreeTrackerDAO
) {
    var convergenceWithinRange: Boolean = false

    private val gson = GsonBuilder().serializeNulls().create()
    private var lastNLocations: Deque<Location> = LinkedList<Location>()
    var generatedTreeUuid: UUID? = null
        private set
    var longitudeMean: Double? = null
        private set
    var longitudeVariance: Double? = null
        private set
    var longitudeStdDev: Double? = null
        private set

    private val locationObserver: Observer<Location?> = Observer {
        it?.apply {

            if (isInTreeCaptureMode() && !(convergenceWithinRange)) {
                var replacedLocation: Location? = null
                if (lastNLocations.size >= 5) {
                    replacedLocation = lastNLocations.pollFirst()
                    Timber.d("Convergence: polling the head from lastNLocations")
                }
                lastNLocations.add(it)
                if (longitudeStdDev == null) {
                    computeInitialConvergence(lastNLocations.toList())
                } else {
                    computeRunningConvergence(replacedLocation!!, it)
                }
                Timber.d("Convergence: Longitude Mean: [$longitudeMean]. \n" +
                        "Longitude standard deviation value: [$longitudeStdDev]")
                longitudeStdDev?.let {
                    convergenceWithinRange = it < 0.00000021000001275237096
                }
            }

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
                Timber.d("Convergence: Generated Location Data value $locationData")
                val jsonValue = gson.toJson(locationData)
                Timber.d("Convergence: Inserting a new location data $jsonValue")
                treeTrackerDAO.insertLocationData(LocationDataEntity(jsonValue))
            }
        }
    }

    fun start() {
        locationUpdateManager.locationUpdateLiveData.observeForever(locationObserver)
    }

    fun isInTreeCaptureMode(): Boolean {
        return generatedTreeUuid != null
    }

    fun turnOnTreeCaptureMode() {
        generatedTreeUuid = UUID.randomUUID()
        Timber.d("Convergence: Tree capture mode turned on")
    }

    fun turnOffTreeCaptureMode() {
        generatedTreeUuid = null
        longitudeStdDev = null
        lastNLocations.clear()
        convergenceWithinRange = false
        Timber.d("Convergence: Tree capture turned off")
    }

    private fun computeRunningConvergence(replaceLocation: Location, newLocation: Location) {
        Timber.d("Convergence: Evaluating running convergence stats")
        // Implementation based on the following answer found here since this seems to be a good
        // approximation to the running window standard deviation calculation. Considered Welford's
        // method of computing variance but it calculates running cumulative variance but we need
        // sliding window computation here.
        // https://math.stackexchange.com/questions/2815732/calculating-standard-deviation-of-a-moving-window
        //
        // Assuming you are using SD with Bessel's correction, call μn and SDn the mean and
        // standard deviation from n to n+99. Then, calculate μ1 and SD1 afterwards, you can use the
        // recursive relation
        //  μn+1=μn−(1/99*X(n))+(1/99*X(n+100)) and
        // variance(n+1)= variance(n) −1/99(Xn−μn)^2 + 1/99(X(n+100)−μ(n+1))^2
        val priorLongitudeMean = longitudeMean
        longitudeMean = priorLongitudeMean!!
            .minus(replaceLocation.longitude / 5)
            .plus(newLocation.longitude / 5)
        longitudeVariance = longitudeVariance!!
            .minus(Math.pow((replaceLocation.longitude - priorLongitudeMean!!), 2.0) / 5)
            .plus(Math.pow((newLocation.longitude - longitudeMean!!), 2.0) / 5)
        longitudeStdDev = Math.sqrt(longitudeVariance!!)
    }

    private fun computeInitialConvergence(locations: List<Location>) {
        Timber.d("Convergence: Evaluating initial convergence stats")
        if (locations.size < 5)
            return

        fun computeLongitudeStats() {
            longitudeMean = locations
                .map { it.longitude }
                .sum()
                .div(locations.size)
            var variance = longitudeVariance ?: 0.0
            for (x in locations) {
                variance += Math.pow((x.longitude - longitudeMean!!), 2.0)
            }
            longitudeVariance = variance
            longitudeStdDev = Math.sqrt(variance / locations.size)
        }

        computeLongitudeStats()
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
