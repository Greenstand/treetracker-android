/*
 * Copyright 2026 Treetracker
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

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.capture.TreeImageReviewViewModel
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.map.MapViewModel
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementViewModel
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.koin.core.error.InstanceCreationException
import org.koin.core.parameter.parametersOf
import kotlin.test.assertFailsWith
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowData
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.overlay.NoOpSyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.settings.SettingsViewModel
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.definition.Kind
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Starts the real app Koin graph (appModule, roomModule, networkModule) the way
 * TreeTrackerApplication does, with only the Firebase, WorkManager and S3 entry points
 * replaced, and checks that every definition can be created. A missing binding otherwise
 * only shows up as a crash when the screen that needs it is first opened.
 */
@OptIn(ExperimentalCoroutinesApi::class, KoinInternalApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class KoinGraphTest {
    // View model init blocks launch on Main; a StandardTestDispatcher keeps them from running,
    // since this test is only about construction.
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule(StandardTestDispatcher())

    private val externalServices =
        module {
            single { mockk<FirebaseAnalytics>(relaxed = true) }
            single { mockk<FirebaseRemoteConfig>(relaxed = true) }
            factory { mockk<FirebaseCrashlytics>(relaxed = true) }
            single { mockk<WorkManager>(relaxed = true) }
            single { mockk<ObjectStorageClient>(relaxed = true) }
        }

    private lateinit var koin: Koin

    @Before
    fun setUp() {
        stopKoin()
        // DeviceUtils reads the Android ID through the application's static context.
        val context = ApplicationProvider.getApplicationContext<Application>()
        TreeTrackerApplication.appContext = context
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "test-device-id")
        // UploadLocationDataUseCase reads the static instance rather than the Koin binding.
        ObjectStorageClient.init(context)
        koin =
            startKoin {
                allowOverride(true)
                androidContext(context)
                modules(appModule, roomModule, networkModule, externalServices)
            }.koin
        // The splash screen does this at startup; capture flow classes read the current org.
        runBlocking { koin.get<OrgRepo>().init() }
    }

    @After
    fun tearDown() {
        CaptureFlowScopeManager.close()
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    @Test
    fun `every root definition in the app graph can be created`() {
        // TreeCapturer at the root is a bridge into the open capture flow scope.
        CaptureFlowScopeManager.open()

        val rootDefinitions =
            koin.instanceRegistry.instances.values
                .map { it.beanDefinition }
                .filter { it.scopeQualifier == koin.scopeRegistry.rootScope.scopeQualifier }
                .distinct()
                .filterNot { it.primaryType in VIEW_MODELS_WITH_ROUTE_ARGS }

        val failures =
            rootDefinitions.mapNotNull { definition ->
                runCatching { koin.get<Any>(definition.primaryType, definition.qualifier) }
                    .exceptionOrNull()
                    ?.let { "${definition.primaryType.simpleName}: ${it.message}" }
            }

        assertTrue(rootDefinitions.size > 50, "Only found ${rootDefinitions.size} definitions")
        if (failures.isNotEmpty()) fail("Could not create:\n" + failures.joinToString("\n"))
    }

    @Test
    fun `message view models resolve when their route arguments are passed as parameters`() {
        assertTrue(koin.get<ChatViewModel> { parametersOf(7L, "other-wallet") } is ChatViewModel)
        assertTrue(koin.get<AnnouncementViewModel> { parametersOf("message-id") } is AnnouncementViewModel)
        assertTrue(
            koin.get<IndividualMessageListViewModel> { parametersOf(7L) } is IndividualMessageListViewModel,
        )
    }

    @Test
    fun `TreeTrackerViewModelFactory builds its view models from the real graph`() {
        CaptureFlowScopeManager.open()
        val factory = koin.get<TreeTrackerViewModelFactory>()

        // Left out: IndividualMessageListViewModel (its branch cannot supply the user id, see
        // VIEW_MODELS_WITH_ROUTE_ARGS) and SplashScreenViewModel (no Koin definition). Both
        // screens build their view model with their own factory, so those branches are unused.
        listOf(
            UserSelectViewModel::class,
            DashboardViewModel::class,
            SignupViewModel::class,
            LanguagePickerViewModel::class,
            OrgPickerViewModel::class,
            WalletSelectViewModel::class,
            AddWalletViewModel::class,
            TreeImageReviewViewModel::class,
            PermissionViewModel::class,
            TreeHeightSelectionViewModel::class,
            AddOrgViewModel::class,
            SessionNoteViewModel::class,
            DevOptionsViewModel::class,
            SettingsViewModel::class,
            MapViewModel::class,
        ).forEach { type ->
            assertTrue(type.isInstance(factory.create(type.java)), "Could not build ${type.simpleName}")
        }
    }

    @Test
    fun `message view models cannot be resolved without their route arguments`() {
        VIEW_MODELS_WITH_ROUTE_ARGS.forEach { type ->
            assertFailsWith<InstanceCreationException>(type.simpleName) { koin.get<Any>(type) }
        }
    }

    @Test
    fun `capture setup scope provides its data and navigation controller`() {
        CaptureSetupScopeManager.open()

        assertSame(CaptureSetupScopeManager.getData(), CaptureSetupScopeManager.getData())
        assertSame(CaptureSetupScopeManager.nav, CaptureSetupScopeManager.nav)
    }

    @Test
    fun `capture flow scope provides its data, navigation controller and tree capturer`() {
        CaptureFlowScopeManager.open()

        val scope = CaptureFlowScopeManager.getData()
        assertSame(scope.get<CaptureFlowData>(), scope.get<CaptureFlowData>())
        assertSame(scope.get<TreeCapturer>(), scope.get<TreeCapturer>())
        assertSame(CaptureFlowScopeManager.nav, CaptureFlowScopeManager.nav)
    }

    @Test
    fun `root TreeCapturer resolves to the one in the open capture flow`() {
        CaptureFlowScopeManager.open()

        assertSame(CaptureFlowScopeManager.getData().get<TreeCapturer>(), koin.get<TreeCapturer>())
    }

    @Test
    fun `reopening the capture flow gives a fresh TreeCapturer`() {
        CaptureFlowScopeManager.open()
        val first = koin.get<TreeCapturer>()

        CaptureFlowScopeManager.open()

        assertNotSame(first, koin.get<TreeCapturer>())
    }

    @Test
    fun `view models are factories so each request gets a new instance`() {
        CaptureFlowScopeManager.open()

        assertNotSame(koin.get<SettingsViewModel>(), koin.get<SettingsViewModel>())
        assertNotSame(koin.get<DashboardViewModel>(), koin.get<DashboardViewModel>())
        assertNotSame(koin.get<TreeImageReviewViewModel>(), koin.get<TreeImageReviewViewModel>())
    }

    @Test
    fun `use cases are factories and the view model factory is a singleton`() {
        assertNotSame(koin.get<SyncDataUseCase>(), koin.get<SyncDataUseCase>())
        assertSame(koin.get<TreeTrackerViewModelFactory>(), koin.get<TreeTrackerViewModelFactory>())
        val definition =
            koin.instanceRegistry.instances.values
                .map { it.beanDefinition }
                .first { it.primaryType == SyncDataUseCase::class }
        assertEquals(Kind.Factory, definition.kind)
    }

    @Test
    fun `sync progress tracker matches the debug feature flag`() {
        val tracker = koin.get<SyncProgressTracker>()

        assertEquals(!FeatureFlags.DEBUG_ENABLED, tracker is NoOpSyncProgressTracker)
    }

    private companion object {
        /**
         * These take a user id / message id as their first constructor argument. Their screens
         * build them with dedicated ViewModelProvider factories, so the Koin definitions only work
         * when the caller passes those arguments with parametersOf.
         */
        val VIEW_MODELS_WITH_ROUTE_ARGS =
            setOf(
                ChatViewModel::class,
                AnnouncementViewModel::class,
                IndividualMessageListViewModel::class,
            )
    }
}