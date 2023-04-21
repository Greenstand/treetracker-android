/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import com.google.gson.Gson
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

class Configuration(
    private val preferences: Preferences,
    private val gson: Gson
) {

    private val LOCATION_DATA_CONFIG_KEY: PrefKey = PrefKeys.SYSTEM_SETTINGS +
        PrefKey("location-data-config")

    var locationDataConfig: LocationDataConfig
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
    val convergenceTimeout: Long = 20000L,
    val convergenceDataSize: Int = 5,
    val lonStdDevThreshold: Float = 0.00001F,
    val latStdDevThreshold: Float = 0.00001F
)