package org.greenstand.android.TreeTracker.permissions

import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PermissionItemsState(
    val isLocationEnabled: Boolean? = null
)
class PermissionViewModel (
    private val locationManager: LocationManager) : ViewModel(){

    private val _state = MutableLiveData(PermissionItemsState(isLocationEnabled = isLocationEnabled()))
    val state: LiveData<PermissionItemsState> = _state

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}