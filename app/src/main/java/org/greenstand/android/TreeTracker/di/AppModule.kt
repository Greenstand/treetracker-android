package org.greenstand.android.TreeTracker.di

import android.content.Context
import android.location.LocationManager
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.usecases.*
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.viewmodels.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {


    viewModel { LoginViewModel(get(), get()) }

    viewModel { SignupViewModel() }

    viewModel { TermsPolicyViewModel(get(), get()) }

    viewModel { TreeHeightViewModel(get(), get(), get()) }

    viewModel { DataViewModel(get(), get(), get(), get()) }

    viewModel { MapViewModel(get(), get(), get(), get(), get()) }

    viewModel { TreePreviewViewModel(get(), get()) }

    viewModel { NewTreeViewModel(get(), get(), get(), get()) }

    single { WorkManager.getInstance(get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { FirebaseAnalytics.getInstance(get()) }

    single { UserManager(get(), get()) }

    single { Analytics(get(), get(), get()) }

    single { DeviceUtils }

    single { SyncNotificationManager(get(), get()) }

    single { androidContext().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE) }

    single { androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    single { UserLocationManager(get(), get()) }

    single { ObjectStorageClient.instance() }

    single { NotificationManagerCompat.from(get()) }

    factory { UploadImageUseCase(get()) }

    factory { UploadTreeUseCase(get(), get(), get()) }

    factory { UploadPlanterUseCase(get(), get(), get(), get()) }

    factory { SyncTreeUseCase(get(), get(), get(), get()) }

    factory { CreateTreeUseCase(get(), get(), get()) }

    factory { CreateFakeTreesUseCase(get(), get(), get(), get()) }

    factory { UploadPlanterCheckInUseCase(get(), get()) }

    factory { CreatePlanterInfoUseCase(get(), get(), get()) }

    factory { CreatePlanterCheckInUseCase(get(), get(), get(), get(), get()) }

    factory { ExpireCheckInStatusUseCase(get()) }

    factory { ValidateCheckInStatusUseCase(get()) }

    factory { PlanterCheckInUseCase(get(), get()) }

    factory { UploadPlanterInfoUseCase(get(), get()) }

    factory { CreateTreeRequestUseCase(get()) }

    factory { UploadTreeBundleUseCase(get(), get(), get(), get(), get()) }

    factory { RemoveLocalTreeImagesWithIdsUseCase(get()) }

    factory { DeleteOldPlanterImagesUseCase(get(), get()) }

    factory<TreeUploadStrategy>(named(BundleTreeUploadStrategy.tag)) {
        BundleTreeUploadStrategy(
            uploadTreeBundleUseCase = get()
        )
    }

    factory<TreeUploadStrategy>(named(ContinuousTreeUploadStrategy.tag)) {
        ContinuousTreeUploadStrategy(
            syncTreeUseCase = get()
        )
    }

    factory(named(SyncDataUseCase.BUNDLE_UPLOAD)) {
        SyncDataUseCase(
            treeLoadStrategy = get(named(BundleTreeUploadStrategy.tag)),
            uploadPlanterDetailsUseCase = get(),
            dao = get()
        )
    }

    factory(named(SyncDataUseCase.CONTINUOUS_UPLOAD)) {
        SyncDataUseCase(
            treeLoadStrategy = get(named(ContinuousTreeUploadStrategy.tag)),
            uploadPlanterDetailsUseCase = get(),
            dao = get()
        )
    }

}