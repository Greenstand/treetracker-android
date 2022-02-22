package org.greenstand.android.TreeTracker.models.location

import android.location.Location
import androidx.lifecycle.Observer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.models.Configuration
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.LocationData
import org.greenstand.android.TreeTracker.models.Planter
import org.greenstand.android.TreeTracker.models.SessionTracker
import timber.log.Timber
import java.util.*

class LocationDataCapturer(
    private val userManager: Planter,
    private val locationUpdateManager: LocationUpdateManager,
    private val treeTrackerDAO: TreeTrackerDAO,
    private val configuration: Configuration,
    private val gson: Gson,
    private val sessionTracker: SessionTracker,
) {
    private var locationsDeque: Deque<Location> = LinkedList()
    var generatedTreeUuid: UUID? = null
        private set
    var lastConvergenceWithinRange: Convergence? = null
        private set
    var currentConvergence: Convergence? = null
        private set
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
                treeTrackerDAO.insertLocationData(
                    LocationEntity(
                        locationDataJson = jsonValue,
                        sessionId = sessionTracker.currentSessionId,
                    )
                )
            }
        }
    }

    fun isLocationCoordinateAvailable(): Boolean {
        return (lastConvergenceWithinRange != null || currentConvergence != null)
    }

    fun startGpsUpdates() {
        locationUpdateManager.startLocationUpdates()
        locationUpdateManager.locationUpdateLiveData.observeForever(locationObserver)
    }

    fun stopGpsUpdates() {
        locationUpdateManager.locationUpdateLiveData.removeObserver(locationObserver)
        locationUpdateManager.stopLocationUpdates()
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