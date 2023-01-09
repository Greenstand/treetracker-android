package org.greenstand.android.TreeTracker.models.location

import android.location.Location
import androidx.annotation.MainThread
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
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import timber.log.Timber
import java.util.*
import kotlin.math.min
import kotlin.properties.Delegates

class LocationDataCapturer(
    private val locationUpdateManager: LocationUpdateManager,
    private val treeTrackerDAO: TreeTrackerDAO,
    private val configuration: Configuration,
    private val gson: Gson,
    private val sessionTracker: SessionTracker,
    private val timeProvider: TimeProvider,
) {
    private var locationsDeque: Deque<Location> = LinkedList()
    var generatedTreeUuid: UUID? = null
        private set
    var lastConvergenceWithinRange: Convergence? = null
        private set
    var currentConvergence: Convergence? = null
        private set
    private var convergenceStatus: ConvergenceStatus? = null
    private var areLocationUpdatesOn: Boolean = false
    val percentageConvergenceObservers = mutableListOf<(Float) -> Unit>()
    var newestPercentageConvergence: Float by Delegates.observable(0f) { _, oldValue, newValue ->
        //convergence percentage fluctuates so it is only updated when it increases
        if (newValue > oldValue) {
            percentageConvergenceObservers.forEach { it(newValue) }
        }
    }

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
                        val minimumConvergenceRatio = min(locationDataConfig.latStdDevThreshold.div(latStdDev).toFloat(),locationDataConfig.lonStdDevThreshold.div(longStdDev).toFloat())
                        newestPercentageConvergence = min(1f,minimumConvergenceRatio)

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

            sessionTracker.currentSessionId?.let { currentSessionId ->

                MainScope().launch(Dispatchers.IO) {
                    val locationData =
                        LocationData(
                            currentSessionId,
                            latitude,
                            longitude,
                            accuracy,
                            generatedTreeUuid?.toString(),
                            convergenceStatus,
                            timeProvider.currentTime().toString(),
                        )
                    val jsonValue = gson.toJson(locationData)
                    Timber.d("Inserting new location data $jsonValue")
                        treeTrackerDAO.insertLocationData(
                            LocationEntity(
                                locationDataJson = jsonValue,
                                sessionId = currentSessionId,
                            )
                        )
                }
            }
        }
    }

    fun isLocationCoordinateAvailable(): Boolean {
        return (lastConvergenceWithinRange != null || currentConvergence != null)
    }

    fun startGpsUpdates() {
        if (areLocationUpdatesOn) {
            return
        }
        locationUpdateManager.startLocationUpdates()
        locationUpdateManager.locationUpdateLiveData.observeForever(locationObserver)
        areLocationUpdatesOn = true
    }

    @MainThread
    fun stopGpsUpdates() {
        if (!areLocationUpdatesOn) {
            return
        }
        locationUpdateManager.locationUpdateLiveData.removeObserver(locationObserver)
        locationUpdateManager.stopLocationUpdates()
        areLocationUpdatesOn = false
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