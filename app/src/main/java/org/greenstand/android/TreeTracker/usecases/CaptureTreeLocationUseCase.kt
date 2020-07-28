package org.greenstand.android.TreeTracker.usecases

import android.location.Location
import android.util.Base64
import androidx.lifecycle.Observer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import timber.log.Timber

class CaptureTreeLocationUseCase(
    private val userManager: UserManager,
    private val userLocationManager: UserLocationManager,
    private val treeTrackerDAO: TreeTrackerDAO
) {

    private val gson = Gson()
    private val locationObserver: Observer<Location?> = Observer {
        it?.apply {
            MainScope().launch(Dispatchers.IO) {
                userManager.planterCheckinId?.let { planterCheckinId ->
                    val locationData =
                        LocationData(
                            planterCheckinId,
                            latitude,
                            longitude,
                            accuracy,
                            System.currentTimeMillis()
                        )
                    val base64String = Base64.encodeToString(
                        gson.toJson(locationData).toByteArray(),
                        Base64.NO_WRAP
                    )
                    Timber.d("Inserting a new location data $base64String")
                    treeTrackerDAO.insertLocationData(LocationDataEntity(base64String))
                }
            }
        }
    }

    fun startLocationCapture() {
        userLocationManager.locationUpdateLiveData.observeForever(locationObserver)
    }

    fun stopLocationCapture() {
        userLocationManager.locationUpdateLiveData.removeObserver(locationObserver)
    }
}

data class LocationData(
    val planterCheckInId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val capturedAt: Long
)
