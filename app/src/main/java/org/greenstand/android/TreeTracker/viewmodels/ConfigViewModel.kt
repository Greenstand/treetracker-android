package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Configuration
import org.greenstand.android.TreeTracker.models.LocationDataConfig

class ConfigViewModel(private val configuration: Configuration) : ViewModel() {

    private val locationConfigLiveData = MutableLiveData<LocationDataConfig>()

    fun getLocationDataConfig(): LiveData<LocationDataConfig> {
        locationConfigLiveData.value = configuration.locationDataConfig
        return locationConfigLiveData
    }

    fun updateLocationDataConfig(locationDataConfig: LocationDataConfig) {
        configuration.updateLocationDataConfig(locationDataConfig)
    }
}
