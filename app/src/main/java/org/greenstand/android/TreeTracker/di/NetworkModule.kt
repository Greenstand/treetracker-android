package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.models.messages.network.MessagesApi
import org.greenstand.android.TreeTracker.models.messages.network.MessagesApiService
import org.greenstand.android.TreeTracker.models.messages.network.RetrofitBuilder
import org.koin.dsl.module
import retrofit2.Retrofit

val networkModule = module {

    single { RetrofitBuilder().create() }

    single { get<Retrofit>().create(MessagesApiService::class.java) }

    single { MessagesApi(get()) }
}