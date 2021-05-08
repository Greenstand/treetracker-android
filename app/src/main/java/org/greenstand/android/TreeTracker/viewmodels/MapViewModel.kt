package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Planter
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.ValidateCheckInStatusUseCase

class MapViewModel constructor(
    private val validateCheckInStatusUseCase: ValidateCheckInStatusUseCase,
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
        if (validateCheckInStatusUseCase.execute(Unit)) {
            checkInStatusLiveData.postValue(true)
        } else {
            user.expireCheckInStatus()
            checkInStatusLiveData.postValue(false)
        }
    }

    suspend fun requiresLogin(): Boolean {
        return !validateCheckInStatusUseCase.execute(Unit)
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
