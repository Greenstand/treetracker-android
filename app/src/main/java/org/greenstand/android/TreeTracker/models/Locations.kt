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
import com.google.gson.Gson
import java.util.Deque
import java.util.LinkedList
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
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

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
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

enum class Accuracy {
    GOOD,
    BAD,
    NONE
}

class LocationDataCapturer(
    private val userManager: Planter,
    private val locationUpdateManager: LocationUpdateManager,
    private val treeTrackerDAO: TreeTrackerDAO,
    private val configuration: Configuration,
    private val gson: Gson
) {
    private var locationsDeque: Deque<Location> = LinkedList()
    var generatedTreeUuid: UUID? = null
        private set
    var lastConvergenceWithinRange: Convergence? = null
    var currentConvergence: Convergence? = null
    private var convergenceStatus: ConvergenceStatus? = null

    private val locationObserver: Observer<Location?> = Observer { location ->
        location?.apply {

            val locationDataConfig = configuration.locationDataConfig
            val convergenceDataSize = locationDataConfig.convergenceDataSize
            if (isInTreeCaptureMode()) {
                val evictedLocation: Location? = if (locationsDeque.size >= convergenceDataSize)
                    locationsDeque.pollFirst() else null
                locationsDeque.add(location)

                if (locationsDeque.size >= convergenceDataSize) {
                    if (currentConvergence == null ||
                        currentConvergence?.locations!!.size < convergenceDataSize
                    ) {
                        currentConvergence = Convergence(locationsDeque.toList())
                        currentConvergence?.computeConvergence()
                    } else {
                        currentConvergence?.computeSlidingWindowConvergence(
                            evictedLocation!!, location
                        )
                    }
                    Timber.d(
                        "Convergence: Longitude Mean: " +
                            "[${currentConvergence?.longitudeConvergence?.mean}]. \n" +
                            "Longitude standard deviation value: " +
                            "[${currentConvergence?.longitudeConvergence?.standardDeviation}]"
                    )
                    Timber.d(
                        "Convergence: Latitude Mean: " +
                            "[${currentConvergence?.latitudeConvergence?.mean}]. \n " +
                            "Latitude standard deviation value: " +
                            "[${currentConvergence?.latitudeConvergence?.standardDeviation}]"
                    )

                    val longStdDev = currentConvergence?.longitudinalStandardDeviation()
                    val latStdDev = currentConvergence?.latitudinalStandardDeviation()
                    if (longStdDev != null && latStdDev != null) {
                        if (longStdDev < locationDataConfig.lonStdDevThreshold &&
                            latStdDev < locationDataConfig.latStdDevThreshold
                        ) {
                            convergenceStatus = ConvergenceStatus.CONVERGED
                            lastConvergenceWithinRange = currentConvergence
                        } else {
                            convergenceStatus = ConvergenceStatus.NOT_CONVERGED
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
        if (!locationUpdateManager.isUpdating) {
            locationUpdateManager.startLocationUpdates()
        }
        locationUpdateManager.locationUpdateLiveData.observeForever(locationObserver)
    }

    /**
     *  Guarantees a Convergence instance that is within the variance threshold or the current
     *  running instance (Not converged) as long as this method is invoked during a tree capture.
     *
     *  @throws IllegalStateException - If invoked outside the scope of tree capture
     */
    fun convergence(): Convergence {
        if (!isInTreeCaptureMode())
            throw IllegalStateException()
        return lastConvergenceWithinRange ?: currentConvergence!!
    }

    suspend fun converge() {
        try {
            val locationDataConfig = configuration.locationDataConfig
            withTimeout(locationDataConfig.convergenceTimeout) {
                while (!isConvergenceWithinRange()) {
                    delay(locationDataConfig.minTimeBetweenUpdates)
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.d("Convergence request timed out")
            convergenceStatus = ConvergenceStatus.TIMED_OUT
        }
    }

    fun isConvergenceWithinRange(): Boolean = ConvergenceStatus.CONVERGED == convergenceStatus

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
        currentConvergence = null
        lastConvergenceWithinRange = null
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

    /**
     * Implementation based on the following answer found in stackexchange since it seems to be a good
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
            (replacingValue / locations.size) +
            (newValue / locations.size)
        val newVariance = currentStats.variance -
            ((replacingValue - currentStats.mean).pow(2.0) / locations.size) +
            ((newValue - newMean).pow(2.0) / locations.size)
        val newStdDev = sqrt(newVariance)
        return ConvergenceStats(newMean, newVariance, newStdDev)
    }

    fun computeConvergence() {
        Timber.d("Convergence: Evaluating initial convergence stats")
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
