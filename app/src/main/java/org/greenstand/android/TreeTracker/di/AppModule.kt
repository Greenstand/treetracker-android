package org.greenstand.android.TreeTracker.di

import android.content.Context
import android.location.LocationManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.analytics.FirebaseAnalytics
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.managers.PlanterManager
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.usecases.*
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.viewmodels.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel { LoginViewModel(get(), get()) }

    viewModel { SignupViewModel() }

    viewModel { TermsPolicyViewModel(get(), get()) }

    viewModel { TreeHeightViewModel(get(), get(), get()) }

    viewModel { DataViewModel(get(), get(), get(), get(), get(), get(), get()) }

    viewModel { MapViewModel(get(), get(), get(), get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { FirebaseAnalytics.getInstance(get()) }

    single { TreeManager(get(), get()) }

    single { UserManager(get(), get(), get()) }

    single { PlanterManager(get(), get()) }

    single { Analytics(get(), get(), get()) }

    single { DeviceUtils }

    single { androidContext().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE) }

    single { androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    single { UserLocationManager(get(), get()) }

    single { DOSpaces.instance() }

    factory { UploadImageUseCase(get()) }

    factory { UploadTreeUseCase(get(), get()) }

    factory { UploadPlanterUseCase(get(), get(), get()) }

    factory { SyncTreeUseCase(get(), get(), get()) }

    factory { CreateTreeUseCase(get(), get(), get()) }

    factory { CreatePlanterInfoUseCase(get(), get(), get()) }

    factory { CreatePlanterCheckInUseCase(get(), get(), get(), get(), get()) }

    factory { ExpireCheckInStatusUseCase(get()) }

    factory { ValidateCheckInStatusUseCase(get()) }

    factory { PlanterCheckInUseCase(get(), get()) }

    factory { SyncDataUseCase(get(), get(), get(), get()) }
}