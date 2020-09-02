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
    var convergence: Convergence? = null

    private val locationObserver: Observer<Location?> = Observer {
        it?.apply {
            if (isInTreeCaptureMode() && !(convergenceWithinRange)) {

                var evictedLocation: Location? = if (lastNLocations.size >= 5)
                    lastNLocations.pollFirst() else null
                lastNLocations.add(it)

                if (lastNLocations.size >= 5) {
                    if (convergence == null) {
                        convergence = Convergence(lastNLocations.toList())
                        convergence!!.computeConvergence()
                    } else {
                        convergence!!.computeRunningConvergence(evictedLocation!!, it)
                    }
                    Timber.d(
                        "Convergence: Longitude Mean: " +
                                "[${convergence!!.longitudeConvergence?.mean}]. \n" +
                                "Longitude standard deviation value: " +
                                "[${convergence!!.longitudeConvergence?.standardDeviation}]"
                    )
                    Timber.d(
                        "Convergence: Latitude Mean: " +
                                "[${convergence!!.latitudeConvergence?.mean}]. \n " +
                                "Latitude standard deviation value: " +
                                "[${convergence!!.latitudeConvergence?.standardDeviation}]"
                    )
                    safeLet(
                        convergence!!.longitudinalStandardDeviation(),
                        convergence!!.latitudinalStandardDeviation()
                    ) { longStdDev, latStdDev ->
                        convergenceWithinRange = longStdDev < 0.00001 &&
                                latStdDev < 0.00001
                    }
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
        convergence = null
        lastNLocations.clear()
        convergenceWithinRange = false
        Timber.d("Convergence: Tree capture turned off")
    }
}

class Convergence(val locations: List<Location>) {

    var longitudeConvergence: ConvergenceStats? = null
        private set
    var latitudeConvergence: ConvergenceStats? = null
        private set

    fun computeStats(data: List<Double>): ConvergenceStats {
        val mean = data.sum().div(data.size)
        var variance = 0.0
        for (x in data) {
            variance += Math.pow((x - mean!!), 2.0)
        }
        variance = variance
        val stdDev = Math.sqrt(variance / data.size)
        return ConvergenceStats(mean, variance, stdDev)
    }

    /**
     * Implementation based on the following answer found here since this seems to be a good
     * approximation to the running window standard deviation calculation. Considered Welford's
     * method of computing variance but it calculates running cumulative variance but we need
     * sliding window computation here.
     *
     * https://math.stackexchange.com/questions/2815732/calculating-standard-deviation-of-a-moving-window
     *
     * Assuming you are using SD with Bessel's correction, call μn and SDn the mean and
     * standard deviation from n to n+99. Then, calculate μ1 and SD1 afterwards, you can use the
     * recursive relation
     *  μn+1=μn−(1/99*X(n))+(1/99*X(n+100)) and
     *  variance(n+1)= variance(n) −1/99(Xn−μn)^2 + 1/99(X(n+100)−μ(n+1))^2
     */
    fun computeSlidingWindowStats(
        currentStats: ConvergenceStats,
        replacingValue: Double,
        newValue: Double
    ): ConvergenceStats {
        val newMean = currentStats.mean
            .minus(replacingValue / 5)
            .plus(newValue / 5)
        val newVariance = currentStats.variance
            .minus(Math.pow((replacingValue - currentStats.mean), 2.0) / 5)
            .plus(Math.pow((newValue - newMean), 2.0) / 5)
        val newStdDev = Math.sqrt(newVariance)
        return ConvergenceStats(newMean, newVariance, newStdDev)
    }

    fun computeConvergence() {
        Timber.d("Convergence: Evaluating initial convergence stats")
        if (locations.size < 5)
            return
        val longitudeData = locations.map { it.longitude }.toList()
        longitudeConvergence = computeStats(longitudeData)

        val latitudeData = locations.map { it.latitude }.toList()
        latitudeConvergence = computeStats(latitudeData)
    }

    fun computeRunningConvergence(replaceLocation: Location, newLocation: Location) {
        Timber.d("Convergence: Evaluating running convergence stats")
        longitudeConvergence = computeSlidingWindowStats(
            longitudeConvergence!!,
            replaceLocation.longitude,
            newLocation.longitude
        )
        latitudeConvergence = computeSlidingWindowStats(
            latitudeConvergence!!,
            replaceLocation.latitude,
            newLocation.latitude
        )
    }

    fun isComputed(): Boolean {
        return longitudeConvergence != null && latitudeConvergence != null
    }

    fun longitudinalStandardDeviation(): Double? {
        return longitudeConvergence?.standardDeviation
    }

    fun latitudinalStandardDeviation(): Double? {
        return latitudeConvergence?.standardDeviation
    }
}

data class ConvergenceStats(
    val mean: Double,
    val variance: Double,
    val standardDeviation: Double
)

data class LocationData(
    val planterCheckInId: Long?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val treeUuid: String?,
    val capturedAt: Long
)

fun <T1 : Any, T2 : Any, R : Any> safeLet(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
    return if (p1 != null && p2 != null) block(p1, p2) else null
}