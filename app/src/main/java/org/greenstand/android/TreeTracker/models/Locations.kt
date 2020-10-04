package org.greenstand.android.TreeTracker.models

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
import java.util.Deque
import java.util.LinkedList
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.utilities.LocationDataConfig
import org.greenstand.android.TreeTracker.utilities.LocationDataConfig.CONVERGENCE_DATA_SIZE
import org.greenstand.android.TreeTracker.utilities.LocationDataConfig.LAT_STD_DEV
import org.greenstand.android.TreeTracker.utilities.LocationDataConfig.LONG_STD_DEV
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

        override fun onLocationChanged(location: Location) {
            currentLocation = location
            Timber.d("Posting location value $currentLocation")
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
                    LocationDataConfig.MIN_TIME_BTWN_UPDATES,
                    LocationDataConfig.MIN_DISTANCE_BTW_UPDATES,
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
    private val userManager: User,
    private val locationUpdateManager: LocationUpdateManager,
    private val treeTrackerDAO: TreeTrackerDAO
) {
    private val gson = GsonBuilder().serializeNulls().create()
    private var locationsDeque: Deque<Location> = LinkedList<Location>()
    var generatedTreeUuid: UUID? = null
        private set
    var convergence: Convergence? = null
        private set
    private var convergenceStatus: ConvergenceStatus? = null

    private val locationObserver: Observer<Location?> = Observer { location ->
        location?.apply {

            if (isInTreeCaptureMode() && !isConvergenceWithinRange()) {

                val evictedLocation: Location? = if (locationsDeque.size >= CONVERGENCE_DATA_SIZE)
                    locationsDeque.pollFirst() else null
                locationsDeque.add(location)

                if (locationsDeque.size >= CONVERGENCE_DATA_SIZE) {
                    if (convergence == null) {
                        convergence = Convergence(locationsDeque.toList())
                        convergence?.computeConvergence()
                    } else {
                        convergence?.computeSlidingWindowConvergence(evictedLocation!!, location)
                    }
                    Timber.d(
                        "Convergence: Longitude Mean: " +
                                "[${convergence?.longitudeConvergence?.mean}]. \n" +
                                "Longitude standard deviation value: " +
                                "[${convergence?.longitudeConvergence?.standardDeviation}]"
                    )
                    Timber.d(
                        "Convergence: Latitude Mean: " +
                                "[${convergence?.latitudeConvergence?.mean}]. \n " +
                                "Latitude standard deviation value: " +
                                "[${convergence?.latitudeConvergence?.standardDeviation}]"
                    )

                    val longStdDev = convergence?.longitudinalStandardDeviation()
                    val latStdDev = convergence?.latitudinalStandardDeviation()
                    if (longStdDev != null && latStdDev != null) {
                        if (longStdDev < LONG_STD_DEV && latStdDev < LAT_STD_DEV) {
                            convergenceStatus = ConvergenceStatus.CONVERGED
                        }
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
                        generatedTreeUuid?.toString(),
                        convergenceStatus,
                        System.currentTimeMillis()
                    )
                val jsonValue = gson.toJson(locationData)
                Timber.d("Inserting new location data $jsonValue")
                treeTrackerDAO.insertLocationData(LocationDataEntity(jsonValue))
            }
        }
    }

    fun start() {
        locationUpdateManager.locationUpdateLiveData.observeForever(locationObserver)
    }

    suspend fun converge() {
        try {
            withTimeout(LocationDataConfig.CONVERGENCE_TIMEOUT) {
                while (!isConvergenceWithinRange()) {
                    delay(LocationDataConfig.MIN_TIME_BTWN_UPDATES)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.d("Convergence request timed out")
            markConvergenceTimeout()
        }
    }

    fun isConvergenceWithinRange(): Boolean = ConvergenceStatus.CONVERGED == convergenceStatus

    /*
     * When the caller (MapFragment via a ViewModel) waits for location convergence to
     * occur and the waiting period exceeds the threshold configured, this method is called
     * to mark the convergence status as timed out for location data pipeline analysis.
     */
    private fun markConvergenceTimeout() {
        convergenceStatus = ConvergenceStatus.TIMED_OUT
    }

    private fun isInTreeCaptureMode(): Boolean {
        return generatedTreeUuid != null
    }

    fun turnOnTreeCaptureMode() {
        generatedTreeUuid = UUID.randomUUID()
        convergenceStatus = ConvergenceStatus.NOT_CONVERGED
        Timber.d("Convergence: Tree capture mode turned on")
    }

    fun turnOffTreeCaptureMode() {
        generatedTreeUuid = null
        convergence = null
        locationsDeque.clear()
        convergenceStatus = null
        Timber.d("Convergence: Tree capture turned off")
    }
}

class Convergence(val locations: List<Location>) {

    var longitudeConvergence: ConvergenceStats? = null
        private set
    var latitudeConvergence: ConvergenceStats? = null
        private set

    private fun computeStats(data: List<Double>): ConvergenceStats {
        val mean = data.sum() / data.size
        var variance = 0.0
        for (x in data) {
            variance += (x - mean).pow(2.0)
        }
        variance /= data.size
        val stdDev = sqrt(variance)
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
        val newMean = currentStats.mean -
                        (replacingValue / CONVERGENCE_DATA_SIZE) +
                        (newValue / CONVERGENCE_DATA_SIZE)
        val newVariance = currentStats.variance -
                        ((replacingValue - currentStats.mean).pow(2.0) / CONVERGENCE_DATA_SIZE) +
                        ((newValue - newMean).pow(2.0) / CONVERGENCE_DATA_SIZE)
        val newStdDev = sqrt(newVariance)
        return ConvergenceStats(newMean, newVariance, newStdDev)
    }

    fun computeConvergence() {
        Timber.d("Convergence: Evaluating initial convergence stats")
        if (locations.size < CONVERGENCE_DATA_SIZE)
            return
        val longitudeData = locations.map { it.longitude }.toList()
        longitudeConvergence = computeStats(longitudeData)

        val latitudeData = locations.map { it.latitude }.toList()
        latitudeConvergence = computeStats(latitudeData)
    }

    fun computeSlidingWindowConvergence(replaceLocation: Location, newLocation: Location) {
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

enum class ConvergenceStatus { CONVERGED, NOT_CONVERGED, TIMED_OUT }

data class LocationData(
    val planterCheckInId: Long?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val treeUuid: String?,
    val convergenceStatus: ConvergenceStatus?,
    val capturedAt: Long
)
