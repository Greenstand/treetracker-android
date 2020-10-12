package org.greenstand.android.TreeTracker.models

import com.google.gson.GsonBuilder
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

class Configuration(private val preferences: Preferences) {

    private val LOCATION_DATA_CONFIG_KEY: PrefKey = PrefKeys.SYSTEM_SETTINGS +
            PrefKey("location-data-config")
    private val gson = GsonBuilder().serializeNulls().create()
    lateinit var locationDataConfig: LocationDataConfig
        private set

    init {
        locationDataConfig = LocationDataConfig() // Initialize with default value
        val locationDataConfigString = preferences.getString(LOCATION_DATA_CONFIG_KEY)
        locationDataConfigString?.let {
            locationDataConfig = gson.fromJson(it, LocationDataConfig::class.java)
        }
    }

    fun updateLocationDataConfig(updatedConfig: LocationDataConfig) {
        preferences.edit().putString(LOCATION_DATA_CONFIG_KEY, gson.toJson(updatedConfig)).apply()
        locationDataConfig = updatedConfig
    }
}

data class LocationDataConfig(
    val minTimeBetweenUpdates: Long = 1000L,
    val minDistanceBetweenUpdates: Float = 0F,
    val configurationTimeout: Long = 60000L,
    val convergenceDataSize: Int = 5,
    val lonStdDevThreshold: Float = 0.00001F,
    val latStdDevThreshold: Float = 0.00001F
)

class LeaveItHereForNow {
    companion object {

        val LOCATION_DATA_CONFIG_KEY = PrefKeys.SYSTEM_SETTINGS +
                PrefKey("location-data-config")
        val MIN_TIME_BTWN_UPDATES = LOCATION_DATA_CONFIG_KEY +
                PrefKey("min-time-between-updates")
        val MIN_DISTANCE_BTWN_UPDATES = LOCATION_DATA_CONFIG_KEY +
                PrefKey("min-distance-btwn-updates")
        val CONFIGURATION_TIMEOUT = LOCATION_DATA_CONFIG_KEY +
                PrefKey("configuration-timeout")
        val CONVERGENCE_DATA_SIZE = LOCATION_DATA_CONFIG_KEY +
                PrefKey("convergence-data-size")
        val LON_STD_DEV_THRESHOLD = LOCATION_DATA_CONFIG_KEY +
                PrefKey("lon-std-dev-threshold")
        val LAT_STD_DEV_THRESHOLD = LOCATION_DATA_CONFIG_KEY +
                PrefKey("lat-std-dev-threshold")
    }
}
