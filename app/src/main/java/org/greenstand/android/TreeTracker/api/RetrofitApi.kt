package org.greenstand.android.TreeTracker.api

import java.io.IOException

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.managers.UserManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class RetrofitApi(val api: ApiService) {

    var authToken: String? = null
        private set

    private fun loggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) })

        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return httpLoggingInterceptor
    }


    suspend fun authenticate(deviceId: String) {
        val result = api.signIn(AuthenticationRequest(deviceAndroidId = deviceId))
        authToken = result.token
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