package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.viewmodels.LoginViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // single instance of HelloRepository
    //single<HelloRepository> { HelloRepositoryImpl() }

    viewModel { LoginViewModel() }
}