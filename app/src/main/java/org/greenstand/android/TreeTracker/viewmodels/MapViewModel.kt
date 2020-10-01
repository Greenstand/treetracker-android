package org.greenstand.android.TreeTracker.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.User
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.ValidateCheckInStatusUseCase

class MapViewModel constructor(
    private val validateCheckInStatusUseCase: ValidateCheckInStatusUseCase,
    private val createFakeTreesUseCase: CreateFakeTreesUseCase,
    private val locationDataCapturer: LocationDataCapturer,
    locationUpdateManager: LocationUpdateManager,
    private val user: User,
    private val stepCounter: StepCounter,
    private val deviceOrientation: DeviceOrientation
) : ViewModel() {

    val checkInStatusLiveData = MutableLiveData<Boolean>()
    val locationUpdates: LiveData<Location?> = locationUpdateManager.locationUpdateLiveData

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
        if (FeatureFlags.TREE_DBH_FEATURE_ENABLED) return

        locationDataCapturer.turnOnTreeCaptureMode()
        stepCounter.enable()
        deviceOrientation.enable()
    }

    suspend fun resolveLocationConvergence() {
        if (FeatureFlags.TREE_DBH_FEATURE_ENABLED) {
            return
        }
        locationDataCapturer.converge()
    }
}
