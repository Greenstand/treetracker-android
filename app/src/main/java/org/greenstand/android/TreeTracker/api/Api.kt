package org.greenstand.android.TreeTracker.api

import java.io.IOException

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class Api {
    private var apiService: ApiService? = null
    private var mOkHttpClient: OkHttpClient? = null
    private var authToken: String? = null

    val api: ApiService?
        get() {
            if (apiService == null) {
                createApi()
            }
            return apiService
        }

    val isLoggedIn: Boolean
        get() = authToken != null

    fun setAuthToken(authToken: String) {
        this.authToken = authToken
    }

    private fun createApi() {
        mOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor())
                .addInterceptor(AuthenticationInterceptor())
                .build()

        apiService = Retrofit.Builder()
                .client(mOkHttpClient!!)
                .baseUrl(ApiService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
    }


    inner class AuthenticationInterceptor : Interceptor {

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {

            val original = chain.request()

            if (authToken != null) {
                val builder = original.newBuilder()
                        .header("Authorization", "Bearer " + authToken!!)
                val request = builder.build()
                return chain.proceed(request)
            } else {
                return chain.proceed(original)
            }

        }

    }

    companion object {

        private var sInstance: Api? = null

        fun instance(): Api {
            if (sInstance == null) {
                sInstance = Api()
            }
            return sInstance!!
        }

        private fun loggingInterceptor(): HttpLoggingInterceptor {
            val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) })

            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            return httpLoggingInterceptor
        }
    }
}