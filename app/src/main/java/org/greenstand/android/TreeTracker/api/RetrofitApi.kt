package org.greenstand.android.TreeTracker.api

import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import timber.log.Timber
import java.io.IOException

class RetrofitApi(private val api: ApiService,
                  private val userManager: UserManager) {

    suspend fun authenticate(deviceId: String) {
        val result = api.signIn(AuthenticationRequest(deviceAndroidId = deviceId))
        userManager.authToken = result.token
    }

    suspend fun updateDevice() {
        api.updateDevice(DeviceRequest())
    }

    suspend fun createTree(newTreeRequest: NewTreeRequest): Int {
        return api.createTree(newTreeRequest).status
    }

    suspend fun authenticateDevice(): Boolean {

        try {
            authenticate(DeviceUtils.deviceId)
            updateDevice()
        } catch (e: IOException) {
            Timber.tag("RetrofitApi").e(e)
            return false
        }

        return true
    }
}