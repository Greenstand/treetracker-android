package org.greenstand.android.TreeTracker.models

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import timber.log.Timber

enum class Accuracy {
    GOOD,
    BAD,
    NONE
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

fun hasGPSDevice(context: Context): Boolean {
    val mgr = context.getSystemService(ComponentActivity.LOCATION_SERVICE) as LocationManager
    val providers = mgr.allProviders
    return providers.contains(LocationManager.GPS_PROVIDER)
}