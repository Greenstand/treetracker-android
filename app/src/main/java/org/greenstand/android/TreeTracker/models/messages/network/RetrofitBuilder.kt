package org.greenstand.android.TreeTracker.models.messages.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitBuilder {

    fun create(): Retrofit {
        return Retrofit.Builder()
            .client(OkHttpClient())
            .baseUrl(BASE_ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    companion object {
        private const val BASE_ENDPOINT = "https://dev-k8s.treetracker.org"
    }

}