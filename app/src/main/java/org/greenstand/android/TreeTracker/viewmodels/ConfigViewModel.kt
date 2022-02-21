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
