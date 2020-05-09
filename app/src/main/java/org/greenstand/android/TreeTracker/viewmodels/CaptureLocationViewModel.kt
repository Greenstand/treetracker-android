package org.greenstand.android.TreeTracker.viewmodels

import android.location.Location
import android.util.Base64
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationCaptureEntity
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class CaptureLocationViewModel(
    private val userManager: UserManager,
    private val userLocationManager: UserLocationManager,
    private val treeTrackerDAO: TreeTrackerDAO,
    private val workManager: WorkManager
):ViewModel() {

    private val gson = Gson()
    private var cancelLocationCaptureJob: CoroutineContext? = null
    private val locationObserver: Observer<Location?> = Observer {
        it?.apply {
            viewModelScope.launch(Dispatchers.IO) {
                userManager.planterCheckinId?.let{ planterCheckinId ->
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
                        Base64.DEFAULT
                    )
                    Timber.i("Inserting a new location data $base64String")
                    treeTrackerDAO.insertLocationData(LocationCaptureEntity(base64String))
                }
            }
        }
    }

    fun startLocationCapture() {
        userLocationManager.locationUpdateLiveDate.observeForever(locationObserver)
    }

    fun stopLocationCapture() {
        cancelLocationCaptureJob?.cancel()
        userLocationManager.locationUpdateLiveDate.removeObserver(locationObserver)
    }
}

data class LocationData(
    val planterCheckInId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val capturedAt: Long
)

