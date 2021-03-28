package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.splash.SplashScreenViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { SplashScreenViewModel(get()) }
    viewModel { DashboardViewModel() }
    viewModel { LanguagePickerViewModel(get(), get()) }
    viewModel { SignupViewModel(get()) }
}
