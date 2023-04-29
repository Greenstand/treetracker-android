/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.di

import android.content.Context
import android.hardware.SensorManager
import android.location.LocationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.capture.TreeImageReviewViewModel
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementViewModel
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.Configuration
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowData
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScope
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.messages.MessageUploader
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.network.MessageTypeDeserializer
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScope
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.usecases.CreateLegacyTreeUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.usecases.UploadLocationDataUseCase
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.GpsUtils
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.viewmodels.ConfigViewModel
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.get
import org.koin.dsl.module

val appModule = module {

    viewModel { SessionNoteViewModel() }

    viewModel { AddWalletViewModel() }

    viewModel { AddOrgViewModel(get()) }

    viewModel { ConfigViewModel(get(), get()) }

    viewModel { LanguagePickerViewModel(get(), get()) }

    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }

    viewModel { OrgPickerViewModel(get()) }

    viewModel { UserSelectViewModel(get(), get(), get()) }

    viewModel { DevOptionsViewModel(get()) }

    viewModel { TreeHeightSelectionViewModel(get()) }

    viewModel { org.greenstand.android.TreeTracker.signup.SignupViewModel(get(), get()) }

    viewModel { WalletSelectViewModel(get()) }

    viewModel { IndividualMessageListViewModel(get(), get(), get()) }

    viewModel { ChatViewModel(get(), get(), get(), get()) }

    viewModel { AnnouncementViewModel(get(), get()) }

    viewModel { TreeImageReviewViewModel(get(), get()) }

    viewModel { PermissionViewModel(get()) }

    single { UserRepo(get(), get(), get(), get(), get(), get()) }

    factory<TreeCapturer> { CaptureFlowScopeManager.getData().get() }

    single { Configurator(get()) }

    single { DeviceConfigUpdater(get(), get()) }

    single { OrgRepo(get(), get(), get()) }

    single { WorkManager.getInstance(get()) }

    single { LocalBroadcastManager.getInstance(get()) }

    single { FirebaseAnalytics.getInstance(get()) }

    single { Analytics(get(), get()) }

    single { DeviceUtils }

    single { GpsUtils(get()) }

    single { SyncNotificationManager(get()) }

    single { androidContext().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE) }

    single { androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    single { androidContext().resources }

    single { MessagesRepo(get(), get(), get(), get(), get()) }

    factory { MessageUploader(get(), get(), get()) }

    single { LocationUpdateManager(get(), get(), get()) }

    single { ObjectStorageClient.instance() }

    single { NotificationManagerCompat.from(get()) }

    single {
        LocationDataCapturer(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    single { Preferences(get()) }

    single {
        ContextCompat.getSystemService(androidContext(), SensorManager::class.java) as SensorManager
    }

    single { SessionTracker(get(), get(), get(), get(), get(), get()) }

    single { StepCounter(get(), get()) }

    single { DeviceOrientation(get()) }

    single { Configuration(get(), get()) }

    single {
        GsonBuilder()
            .registerTypeAdapter(MessageType::class.java, MessageTypeDeserializer())
            .serializeNulls()
            .create()
    }

    single { TreeTrackerViewModelFactory() }

    single { ExceptionDataCollector(get()) }

    factory { TimeProvider(get()) }

    factory { TreesToSyncHelper(get(), get()) }

    factory { PlanterUploader(get(), get(), get(), get()) }

    factory { SessionUploader(get(), get(), get()) }

    factory { DeviceConfigUploader(get(), get(), get()) }

    factory { LanguageSwitcher(get()) }

    factory { UploadImageUseCase(get()) }

    factory { UploadLocationDataUseCase(get(), get()) }

    factory { CreateTreeUseCase(get(), get(), get()) }

    factory { CreateLegacyTreeUseCase(get(), get()) }

    factory { CreateFakeTreesUseCase(get(), get(), get(), get(), get(), get(), get()) }

    factory { CheckForInternetUseCase() }

    factory { CreateTreeRequestUseCase(get()) }

    factory { TreeUploader(get(), get(), get(), get(), get()) }

    factory { SyncDataUseCase(get(), get(), get(), get(), get(), get(), get()) }

    factory { Firebase.crashlytics }

    scope<CaptureSetupScope> {
        scoped { CaptureSetupData(get()) }
        scoped { CaptureSetupNavigationController(get(), get(), get(), get()) }
    }

    scope<CaptureFlowScope> {
        scoped { CaptureFlowData() }
        scoped { CaptureFlowNavigationController(get(), get(), get(), get(), get()) }
        scoped { TreeCapturer(get(), get(), get(), get(), get()) }
    }
}