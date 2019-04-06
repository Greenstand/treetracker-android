package org.greenstand.android.TreeTracker.api

import android.content.ContentValues
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
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
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .build()
                .create(ApiService::class.java)
    }

    private fun loggingInterceptor(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) })

        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return httpLoggingInterceptor
    }


    suspend fun authenticate(deviceId: String) {
        api.signIn(AuthenticationRequest(deviceAndroidId = deviceId)).await()
            .also { authToken = it.token }
    }

    suspend fun updateDevice() {
        api.updateDevice(DeviceRequest()).await()
    }

    suspend fun createPlanterRegistration(registrationRequest: RegistrationRequest): PostResult {
        return api.createPlanterRegistration(registrationRequest).await()
    }

    suspend fun createTree(newTreeRequest: NewTreeRequest): Int {
        return api.createTree(newTreeRequest).await().status
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