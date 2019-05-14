package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.viewmodels.LoginViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel { LoginViewModel() }
}