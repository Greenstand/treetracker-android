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
package org.greenstand.android.TreeTracker.models.setupflow

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.di.appModule
import org.greenstand.android.TreeTracker.di.networkModule
import org.greenstand.android.TreeTracker.di.roomModule
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Process death on the capture setup steps after user select (session note, wallet select and
 * add wallet). Android kills the app in the background and Navigation 3 restores the step's
 * screen, but [CaptureSetupScopeManager] only kept the setup scope in memory, so it is closed.
 *
 * The view models come from the real app Koin graph through [TreeTrackerViewModelFactory], the
 * way the screens get them. Only the user and org repositories are replaced, so a step's user
 * is known and the org's setup flow has the step in it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class CaptureSetupProcessDeathTest {
    // Init blocks launch on Main; an unconfined dispatcher runs them while the view model is built.
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val user =
        User(
            id = 42L,
            wallet = "5551230001",
            numberOfTrees = 0,
            firstName = "Ada",
            lastName = "Admin",
            photoPath = "/selfies/ada.jpg",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    private val userRepo =
        mockk<UserRepo>(relaxed = true) {
            coEvery { getUser(user.id) } returns user
            coEvery { getUserList() } returns listOf(user)
            every { users() } returns flowOf(listOf(user))
        }

    private val orgRepo =
        mockk<OrgRepo>(relaxed = true) {
            every { currentOrg() } returns
                Org(
                    id = "e2e-setup",
                    name = "E2E setup",
                    walletId = "",
                    logoPath = "",
                    captureSetupFlow =
                        listOf(
                            Destination(RouteRegistry.ROUTE_USER_SELECT),
                            Destination(RouteRegistry.ROUTE_SESSION_NOTE),
                            Destination(RouteRegistry.ROUTE_WALLET_SELECT),
                        ),
                    captureFlow = emptyList(),
                )
        }

    private lateinit var koin: Koin

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Application>()
        TreeTrackerApplication.appContext = context
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "test-device-id")
        ObjectStorageClient.init(context)
        koin =
            startKoin {
                allowOverride(true)
                androidContext(context)
                modules(
                    appModule,
                    roomModule,
                    networkModule,
                    module {
                        single { mockk<FirebaseAnalytics>(relaxed = true) }
                        single { mockk<FirebaseRemoteConfig>(relaxed = true) }
                        factory { mockk<FirebaseCrashlytics>(relaxed = true) }
                        single { mockk<WorkManager>(relaxed = true) }
                        single { mockk<ObjectStorageClient>(relaxed = true) }
                        single { userRepo }
                        single { orgRepo }
                    },
                )
            }.koin
    }

    @After
    fun tearDown() {
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    @Test
    fun `session note restored after process death shows the setup user instead of crashing`() {
        val restored = reopenAfterProcessDeath(SessionNoteViewModel::class.java, "Session note")

        assertEquals(
            "Session note did not get its setup user back after process death",
            user.photoPath,
            restored.state.value.userImagePath,
        )
        assertSetupScopeRestored()
    }

    @Test
    fun `wallet select restored after process death shows the setup user instead of crashing`() {
        val restored = reopenAfterProcessDeath(WalletSelectViewModel::class.java, "Wallet select")

        assertEquals(
            "Wallet select did not get its setup user back after process death",
            user,
            restored.state.value.currentUser,
        )
        assertSetupScopeRestored()
    }

    @Test
    fun `add wallet restored after process death shows the setup user instead of crashing`() {
        val restored = reopenAfterProcessDeath(AddWalletViewModel::class.java, "Add wallet")

        assertEquals(
            "Add wallet did not get its setup user back after process death",
            user.photoPath,
            restored.state.value.userImagePath,
        )
        assertSetupScopeRestored()
    }

    /**
     * Opens setup for [user] the way user select does and shows the step once. Then the process
     * dies: the in-memory setup scope is gone, and Android restores the step, building a new view
     * model. Fails if building it crashes.
     */
    private fun <VM : ViewModel> reopenAfterProcessDeath(
        type: Class<VM>,
        step: String,
    ): VM {
        val factory = koin.get<TreeTrackerViewModelFactory>()
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().user = user
        factory.create(type)

        CaptureSetupScopeManager.close()

        val crashes = mutableListOf<Throwable>()
        val thread = Thread.currentThread()
        val previousHandler = thread.uncaughtExceptionHandler
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> crashes += e }
        val restored =
            try {
                runCatching { factory.create(type) }.onFailure { crashes += it }.getOrNull()
            } finally {
                thread.uncaughtExceptionHandler = previousHandler
            }

        assertNull(
            "$step crashed when Android restored it after process death: ${crashes.firstOrNull()}",
            crashes.firstOrNull(),
        )
        return checkNotNull(restored)
    }

    private fun assertSetupScopeRestored() {
        assertTrue("The capture setup scope was not reopened after process death", CaptureSetupScopeManager.isOpen)
        assertEquals(user, CaptureSetupScopeManager.getData().user)
    }
}