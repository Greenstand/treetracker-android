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
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.background.SyncNotificationManager
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowData
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScope
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScope
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.NoOpSyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.GpsUtils
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.get
import org.koin.dsl.module

val utilityModule =
    module {

        factory<TreeCapturer> { CaptureFlowScopeManager.getData().get() }

        single { Configurator(get()) }

        single { DeviceConfigUpdater(get(), get()) }

        single { WorkManager.getInstance(get()) }

        single { FirebaseAnalytics.getInstance(get()) }

        single { Analytics(get(), get()) }

        single { DeviceUtils }

        single { GpsUtils(get()) }

        single { SyncNotificationManager(get()) }

        single { androidContext().getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE) }

        single { androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

        single { androidContext().resources }

        single { NotificationManagerCompat.from(get()) }

        single { Preferences(get()) }

        single {
            ContextCompat.getSystemService(androidContext(), SensorManager::class.java) as SensorManager
        }

        single { SessionTracker(get(), get(), get(), get(), get(), get()) }

        single { StepCounter(get(), get()) }

        single { DeviceOrientation(get()) }

        single { ConvergenceConfiguration(get()) }

        single {
            Json {
                explicitNulls = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }

        single { TreeTrackerViewModelFactory() }

        single<SyncProgressTracker> {
            if (FeatureFlags.DEBUG_ENABLED) SyncProgressTracker() else NoOpSyncProgressTracker()
        }

        single { DebugOverlayManager() }

        single { SensorDiagnosticsTracker(get(), get(), get(), get(), get()) }

        single { ExceptionDataCollector(get()) }

        factory { TimeProvider(get()) }

        factory { TreesToSyncHelper(get(), get()) }

        factory { LanguageSwitcher(get()) }

        factory { Firebase.crashlytics }

        single { FirebaseRemoteConfig.getInstance() }

        scope<CaptureSetupScope> {
            scoped { CaptureSetupData(get()) }
            scoped { CaptureSetupNavigationController(get(), get(), get(), get(), get()) }
        }

        scope<CaptureFlowScope> {
            scoped { CaptureFlowData() }
            scoped { CaptureFlowNavigationController(get(), get(), get(), get(), get(), get()) }
            scoped { TreeCapturer(get(), get(), get(), get(), get()) }
        }
    }