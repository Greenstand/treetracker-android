package org.greenstand.android.TreeTracker.di

import android.content.Context
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.GsonBuilder
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.models.*
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.preferences.PreferencesMigrator
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.CreatePlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.CreatePlanterInfoUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.usecases.PlanterCheckInUseCase
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.usecases.UploadLocationDataUseCase
import org.greenstand.android.TreeTracker.usecases.ValidateCheckInStatusUseCase
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.viewmodels.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel { LoginViewModel(get(), get()) }

    viewModel { SignupViewModel() }

    viewModel { TermsPolicyViewModel(get(), get()) }

    viewModel { TreeHeightViewModel(get(), get(), get(), get()) }

    viewModel { DataViewModel(get(), get(), get(), get()) }

    viewModel { MapViewModel(get(), get(), get(), get(), get(), get(), get()) }

    viewModel { TreePreviewViewModel(get(), get()) }

    viewModel { NewTreeViewModel(get(), get(), get(), get(), get(), get()) }

    viewModel { ConfigViewModel(get(), get()) }

    viewModel { LanguagePickerViewModel(get(), get()) }

    viewModel { DashboardViewModel() }

    viewModel { OrgPickerViewModel(get()) }

    viewModel { UserSelectViewModel(get()) }

    viewModel { org.greenstand.android.TreeTracker.signup.SignupViewModel() }

    single { Users(get(), get(), get()) }

    single<Organizations> { OrganizationsFake() }

    single { WorkManager.getInstance(get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { FirebaseAnalytics.getInstance(get()) }

    single { User(get()) }

    single { Analytics(get(), get(), get()) }

    single { DeviceUtils }

    single { SyncNotificationManager(get(), get()) }

    single { androidContext().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE) }

    single { androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    single { androidContext().resources }

    single { LocationUpdateManager(get(), get(), get()) }

    single { ObjectStorageClient.instance() }

    single { NotificationManagerCompat.from(get()) }

    single {
        LocationDataCapturer(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single { Preferences(get()) }

    single {
        ContextCompat.getSystemService(androidContext(), SensorManager::class.java) as SensorManager
    }

    single { StepCounter(get(), get()) }

    single { DeviceOrientation(get()) }

    single { Configuration(get(), get()) }

    single { GsonBuilder().serializeNulls().create() }

    factory { PlanterUploader(get(), get(), get(), get(), get()) }

    factory { PreferencesMigrator(get(), get()) }

    factory { LanguageSwitcher(get()) }

    factory { UploadImageUseCase(get()) }

    factory { UploadLocationDataUseCase(get(), get()) }

    factory { CreateTreeUseCase(get(), get(), get()) }

    factory { CreateFakeTreesUseCase(get(), get(), get(), get()) }

    factory { CreatePlanterInfoUseCase(get(), get(), get()) }

    factory { CreatePlanterCheckInUseCase(get(), get(), get(), get()) }

    factory { ValidateCheckInStatusUseCase(get()) }

    factory { PlanterCheckInUseCase(get(), get()) }

    factory { CreateTreeRequestUseCase(get()) }

    factory { TreeUploader(get(), get(), get(), get(), get()) }

    factory { SyncDataUseCase(get(), get(), get(), get()) }
}
