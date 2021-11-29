package org.greenstand.android.TreeTracker.permission

import android.location.LocationManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class PermissionItemsState(
    val openAppSettings: Boolean = true,
    val isLocationEnabled: Boolean? = null
)
class PermissionViewModel (
    private val locationManager: LocationManager) : ViewModel(){

    private val _state = MutableLiveData(PermissionItemsState(isLocationEnabled = isLocationEnabled()))
    val state: LiveData<PermissionItemsState> = _state

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    fun setAppSettings(setting : Boolean){
        _state.value = _state.value?.copy(openAppSettings = setting)
    }


}