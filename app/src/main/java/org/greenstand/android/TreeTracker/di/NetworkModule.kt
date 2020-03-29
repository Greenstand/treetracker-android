package org.greenstand.android.TreeTracker.di

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.greenstand.android.TreeTracker.api.ApiService
import org.greenstand.android.TreeTracker.api.AuthenticationInterceptor
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber


val networkModule = module {

    single { RetrofitApi(get(), get()) }

    // Create OkHttp instance
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .addInterceptor(get<AuthenticationInterceptor>())
            .build()
    }

    single {
        val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Timber.tag("OkHttp").d(message) })
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        httpLoggingInterceptor
    }

    single { AuthenticationInterceptor(get()) }

    // Create ApiService instance using Retrofit
    single {
        Retrofit.Builder()
            .client(get())
            .baseUrl(ApiService.ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    single(named("paymentApi")) {
        Retrofit.Builder()
            .client(get())
            .baseUrl(ApiService.PAYMENT_ENDPOINT)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    single(named("planterAccountApi")) {
        RetrofitApi(get(named("paymentApi")), get())
    }

}