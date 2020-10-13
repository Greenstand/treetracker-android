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
    val convergenceTimeout: Long = 60000L,
    val convergenceDataSize: Int = 5,
    val lonStdDevThreshold: Float = 0.00001F,
    val latStdDevThreshold: Float = 0.00001F
)
