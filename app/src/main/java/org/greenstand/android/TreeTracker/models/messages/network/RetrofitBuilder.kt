package org.greenstand.android.TreeTracker.models.messages.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitBuilder {

    fun create(): Retrofit {
        return Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor()
                    .also { it.setLevel(HttpLoggingInterceptor.Level.BODY) })
                .build())
            .baseUrl(BASE_ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        private const val BASE_ENDPOINT = "https://dev-k8s.treetracker.org"
    }

}