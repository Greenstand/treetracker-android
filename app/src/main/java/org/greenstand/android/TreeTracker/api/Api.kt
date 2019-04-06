package org.greenstand.android.TreeTracker.api

import android.content.ContentValues
import java.io.IOException

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.api.models.responses.TokenResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

object Api {

    var authToken: String? = null
        private set

    private val api: ApiService by lazy { createApi() }

    private fun createApi(): ApiService {
        val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor())
                .addInterceptor(AuthenticationInterceptor())
                .build()

        return Retrofit.Builder()
                .client(client)
                .baseUrl(ApiService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
    }

    private fun loggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) })

        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return httpLoggingInterceptor
    }


    suspend fun authenticate(deviceId: String): Boolean {

        val result = api.signIn(AuthenticationRequest(deviceAndroidId = deviceId)).execute()

        if (!result.isSuccessful) {
            return false
        }

        authToken = result.body()?.token!!

        return true
    }

    suspend fun updateDevice(): Boolean {
        val result = api.updateDevice(DeviceRequest()).execute()

        Timber.e("Device Message: ${result.message()}")
        Timber.e("Device Code: ${result.code()}")
        Timber.e("Device Code: ${result.errorBody()?.string()}")

        return result.isSuccessful
    }

    suspend fun createPlanterRegistration(registrationRequest: RegistrationRequest): retrofit2.Response<PostResult> {
        return api.createPlanterRegistration(registrationRequest).execute()
    }

    suspend fun createTree(newTreeRequest: NewTreeRequest): Int? {
        val result = api.createTree(newTreeRequest).execute()

        if (result.isSuccessful) {
            return result.body()!!.status
        }

        return null
    }
}

private class AuthenticationInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {

        val original = chain.request()

        if (Api.authToken != null) {
            val request = original.newBuilder()
                .header("Authorization", "Bearer ${Api.authToken}")
                .build()
            return chain.proceed(request)
        } else {
            return chain.proceed(original)
        }

    }

}