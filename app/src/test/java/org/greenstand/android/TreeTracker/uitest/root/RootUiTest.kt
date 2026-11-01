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
package org.greenstand.android.TreeTracker.uitest.root

import android.Manifest
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.InstallReferrerOrgProvider
import org.greenstand.android.TreeTracker.models.organization.OrgConfigProvider
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.navigation.SplashRoute
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsState
import org.greenstand.android.TreeTracker.overlay.SensorDiagnosticsTracker
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.root.Host
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Tests for [Root] (the org gate in front of the app) and [Host] (the navigation host), with
 * every dependency of the start route's splash screen bound in Koin as a MockK double.
 */
@OptIn(ExperimentalComposeApi::class)
class RootUiTest : RootScreenUiTest() {
    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val treesToSyncHelper = mockk<TreesToSyncHelper>(relaxed = true)
    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val orgConfigProvider = mockk<OrgConfigProvider>(relaxed = true)
    private val installReferrerOrgProvider = mockk<InstallReferrerOrgProvider>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>()
    private val sensorDiagnosticsTracker = mockk<SensorDiagnosticsTracker>(relaxed = true)

    private val orgLoaded = CompletableDeferred<Unit>()

    @Before
    fun setUp() {
        coEvery { orgRepo.ensureInitialized() } coAnswers { orgLoaded.await() }
        coEvery { checkForInternetUseCase.execute(Unit) } returns false
        coEvery { installReferrerOrgProvider.consume() } returns null
        coEvery { orgConfigProvider.fetchOrgConfig(any()) } returns null
        coEvery { userRepo.getPowerUser() } returns null
        every { sessionTracker.wasSessionInterrupted() } returns false
        every { treesToSyncHelper.getTreeCountToSync() } returns 0
        every { sensorDiagnosticsTracker.state } returns MutableStateFlow(SensorDiagnosticsState())

        startKoinWith(
            languageButtonModule(),
            module {
                single { orgRepo }
                single { exceptionDataCollector }
                single { userRepo }
                single { treesToSyncHelper }
                single { sessionTracker }
                single { mockk<DeviceConfigUpdater>(relaxed = true) }
                single { mockk<LocationDataCapturer>(relaxed = true) }
                single { mockk<MessagesRepo>(relaxed = true) }
                single { checkForInternetUseCase }
                single { orgConfigProvider }
                single { installReferrerOrgProvider }
                single { DebugOverlayManager() }
                single { SyncProgressTracker() }
                single { sensorDiagnosticsTracker }
            },
        )
    }

    private fun grantStartupPermissions() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    private fun renderRoot(startRoute: SplashRoute = SplashRoute()) {
        setRootContent { Root(viewModelFactory = TreeTrackerViewModelFactory(), startRoute = startRoute) }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `root loads the org before showing any screen`() {
        renderRoot()

        coVerify(exactly = 1) { orgRepo.ensureInitialized() }
        verify(exactly = 0) { exceptionDataCollector.setScreen(any()) }
        composeTestRule.onNodeWithText(DEBUG_TOGGLE).assertDoesNotExist()
    }

    @Test
    fun `root opens the splash route once the org is loaded`() {
        renderRoot()

        orgLoaded.complete(Unit)

        composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
            runCatching { verify { exceptionDataCollector.setScreen(SPLASH_SCREEN) } }.isSuccess
        }
    }

    @Test
    fun `host reports the start route as the current screen`() {
        orgLoaded.complete(Unit)

        setRootContent { Host() }
        composeTestRule.waitForIdle()

        verify { exceptionDataCollector.setScreen(SPLASH_SCREEN) }
    }

    @Test
    fun `host shows the debug overlay toggle in debug builds`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        orgLoaded.complete(Unit)

        setRootContent { Host() }

        composeTestRule.onNodeWithText(DEBUG_TOGGLE).assertIsDisplayed()
    }

    @Test
    fun `org deep link start route adds the linked org and continues to language select`() {
        grantStartupPermissions()
        orgLoaded.complete(Unit)

        renderRoot(startRoute = SplashRoute(orgId = ORG_ID, orgName = ORG_NAME))
        composeTestRule.mainClock.advanceTimeBy(SPLASH_DELAY_MS)

        composeTestRule.waitUntil(WAIT_TIMEOUT_MS) {
            runCatching { verify { exceptionDataCollector.setScreen(LANGUAGE_SCREEN) } }.isSuccess
        }
        coVerify { orgRepo.addMinimalOrg(ORG_ID, ORG_NAME) }
        coVerify(exactly = 0) { installReferrerOrgProvider.consume() }
    }

    private companion object {
        const val DEBUG_TOGGLE = "D"
        const val SPLASH_SCREEN = "SplashRoute"
        const val LANGUAGE_SCREEN = "LanguageRoute"
        const val ORG_ID = "org-7"
        const val ORG_NAME = "Acme Forestry"
        const val SPLASH_DELAY_MS = 1_000L
        const val WAIT_TIMEOUT_MS = 5_000L
    }
}