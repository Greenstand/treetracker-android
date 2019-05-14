package org.greenstand.android.TreeTracker.api

import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.managers.UserManager

class RetrofitApi(private val api: ApiService,
                  private val userManager: UserManager) {

    suspend fun authenticate(deviceId: String) {
        val result = api.signIn(AuthenticationRequest(deviceAndroidId = deviceId))
        userManager.authToken = result.token
    }

    suspend fun updateDevice() {
        api.updateDevice(DeviceRequest())
    }

    suspend fun createPlanterRegistration(registrationRequest: RegistrationRequest): PostResult {
        return api.createPlanterRegistration(registrationRequest)
    }

    suspend fun createTree(newTreeRequest: NewTreeRequest): Int {
        return api.createTree(newTreeRequest).status
    }
}