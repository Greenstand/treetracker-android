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
package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Configuration
import org.greenstand.android.TreeTracker.models.LocationDataConfig
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager

class ConfigViewModel(
    private val configuration: Configuration,
    private val locationUpdateManager: LocationUpdateManager
) : ViewModel() {

    private val locationConfigLiveData = MutableLiveData<LocationDataConfig>()

    fun getLocationDataConfig(): LiveData<LocationDataConfig> {
        locationConfigLiveData.value = configuration.locationDataConfig
        return locationConfigLiveData
    }

    fun updateLocationDataConfig(locationDataConfig: LocationDataConfig) {
        configuration.updateLocationDataConfig(locationDataConfig)
        locationConfigLiveData.value = locationDataConfig
        // The following call to refresh location update request is required to
        // reflect the newer values for time between location updates parameter
        locationUpdateManager.refreshLocationUpdateRequest()
    }
}
