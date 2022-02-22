package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Planter
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase

@Deprecated("")
class MapViewModel constructor(
    private val createFakeTreesUseCase: CreateFakeTreesUseCase,
    private val locationDataCapturer: LocationDataCapturer,
    locationUpdateManager: LocationUpdateManager,
    private val user: Planter,
    private val stepCounter: StepCounter,
    private val deviceOrientation: DeviceOrientation
) : ViewModel() {

    val checkInStatusLiveData = MutableLiveData<Boolean>()

    init {
        locationUpdateManager.startLocationUpdates()
    }

    suspend fun checkForValidUser() {

    }

    suspend fun requiresLogin(): Boolean {
        return false
    }

    fun getPlanterName(): String {
        return user.firstName + " " + user.lastName
    }

    suspend fun createFakeTrees(): Boolean {
        createFakeTreesUseCase.execute(CreateFakeTreesParams(500))
        return true
    }

    fun turnOnTreeCaptureMode() {
        deviceOrientation.enable()
        if (FeatureFlags.TREE_DBH_FEATURE_ENABLED) return
        locationDataCapturer.turnOnTreeCaptureMode()
        stepCounter.enable()
    }

    suspend fun resolveLocationConvergence() {
        if (FeatureFlags.TREE_DBH_FEATURE_ENABLED) {
            return
        }
        locationDataCapturer.converge()
    }
}
