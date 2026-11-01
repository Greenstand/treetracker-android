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
package org.greenstand.android.TreeTracker.uitest.splash

import android.Manifest
import androidx.compose.ui.test.onRoot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.InstallReferrerOrgProvider
import org.greenstand.android.TreeTracker.models.organization.OrgConfigProvider
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.SplashRoute
import org.greenstand.android.TreeTracker.splash.Splash
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.greenstand.android.TreeTracker.splash.SplashScreenViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

class SplashUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val treesToSyncHelper = mockk<TreesToSyncHelper>(relaxed = true)
    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val deviceConfigUpdater = mockk<DeviceConfigUpdater>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>()
    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private val orgConfigProvider = mockk<OrgConfigProvider>(relaxed = true)
    private val installReferrerOrgProvider = mockk<InstallReferrerOrgProvider>()
    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)

    private val powerUser =
        User(
            id = 1L,
            wallet = "power@user.org",
            numberOfTrees = 0,
            firstName = "Power",
            lastName = "User",
            photoPath = "",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    @Before
    fun setUp() {
        backStack.clear()
        backStack.add(SplashRoute())
        coEvery { checkForInternetUseCase.execute(Unit) } returns false
        coEvery { installReferrerOrgProvider.consume() } returns null
        every { sessionTracker.wasSessionInterrupted() } returns false
        every { treesToSyncHelper.getTreeCountToSync() } returns 0
    }

    private fun createViewModel(): SplashScreenViewModel =
        SplashScreenViewModel(
            orgId = null,
            orgName = null,
            userRepo = userRepo,
            treesToSyncHelper = treesToSyncHelper,
            sessionTracker = sessionTracker,
            deviceConfigUpdater = deviceConfigUpdater,
            locationDataCapturer = locationDataCapturer,
            messagesRepo = messagesRepo,
            checkForInternetUseCase = checkForInternetUseCase,
            orgRepo = orgRepo,
            orgConfigProvider = orgConfigProvider,
            installReferrerOrgProvider = installReferrerOrgProvider,
            exceptionDataCollector = exceptionDataCollector,
        )

    private fun grantStartupPermissions() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    private fun launchSplash() {
        val viewModel = createViewModel()
        setContent(inspectionMode = false) {
            SplashScreen(orgId = null, viewModel = viewModel, navigator = navigator)
        }
        composeTestRule.waitForIdle()
        // SplashScreen waits one second after bootstrapping before navigating.
        composeTestRule.mainClock.advanceTimeBy(SPLASH_DELAY_MS)
    }

    @Test
    fun `splash renders without a view model`() {
        setContent { Splash() }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `permissions granted and no power user navigates to language select and starts gps`() {
        grantStartupPermissions()
        coEvery { userRepo.getPowerUser() } returns null

        launchSplash()

        composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { backStack.lastOrNull() is LanguageRoute }
        assertEquals(listOf(LanguageRoute(isFromTopBar = false)), backStack.toList())
        verify { locationDataCapturer.startGpsUpdates() }
        coVerify { deviceConfigUpdater.saveLatestConfig() }
    }

    @Test
    fun `permissions granted and existing power user navigates to dashboard`() {
        grantStartupPermissions()
        coEvery { userRepo.getPowerUser() } returns powerUser

        launchSplash()

        composeTestRule.waitUntil(WAIT_TIMEOUT_MS) { backStack.lastOrNull() == DashboardRoute }
        assertEquals(listOf(DashboardRoute), backStack.toList())
        verify(exactly = 0) { locationDataCapturer.startGpsUpdates() }
    }

    @Test
    fun `permissions not granted keeps splash and skips bootstrap`() {
        coEvery { userRepo.getPowerUser() } returns null

        launchSplash()
        composeTestRule.waitForIdle()

        assertEquals(listOf(SplashRoute()), backStack.toList())
        coVerify(exactly = 0) { deviceConfigUpdater.saveLatestConfig() }
    }

    private companion object {
        const val SPLASH_DELAY_MS = 1_000L
        const val WAIT_TIMEOUT_MS = 5_000L
    }
}