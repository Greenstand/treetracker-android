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
package org.greenstand.android.TreeTracker.capture

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.userselect.startCaptureSetup
import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Capture setup passes through screens owned by other features (the language picker, sign-up)
 * and comes back to carry on. These tests walk those journeys with the real flow controller.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureSetupFlowTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val clock = FakeClock()
    private val user = FakeFileGenerator.fakeUsers.first()

    @Before
    fun setUp() {
        val orgRepo = mockk<OrgRepo>()
        every { orgRepo.currentOrg() } returns
            Org(
                id = "org",
                name = "Org",
                walletId = "wallet",
                logoPath = "",
                captureSetupFlow =
                    listOf(
                        Destination(RouteRegistry.ROUTE_USER_SELECT),
                        Destination(RouteRegistry.ROUTE_WALLET_SELECT),
                        Destination(RouteRegistry.ROUTE_ADD_ORG),
                        Destination(RouteRegistry.ROUTE_SESSION_NOTE),
                    ),
                captureFlow = emptyList(),
            )
        startKoin {
            modules(
                captureModule,
                module {
                    single { orgRepo }
                    single { sessionTracker }
                    single<StepCounter> { mockk(relaxed = true) }
                    single<LocationDataCapturer> { mockk(relaxed = true) }
                    single<DeviceOrientation> { mockk(relaxed = true) }
                    single<ExceptionDataCollector> { mockk(relaxed = true) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    // Each screen navigates once the previous navigation has settled.
    private fun settle() = clock.advance(300)

    @Test
    fun `switching language on user select, then going through setup, reaches capture`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            // The language button opens the picker; the picker goes back when done.
            settle()
            navigator.throttledNavigate(LanguageRoute(isFromTopBar = true))
            settle()
            navigator.throttledPopBackStack()
            assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)

            settle()
            startCaptureSetup(user, navigator)
            assertEquals(WalletSelectRoute, navigator.topKey)

            CaptureSetupScopeManager.getData().organizationName = "My org"
            CaptureSetupScopeManager.getData().sessionNote = "a note"
            settle()
            CaptureSetupScopeManager.nav.navForward(navigator)
            assertEquals(AddOrgRoute, navigator.topKey)
            settle()
            CaptureSetupScopeManager.nav.navForward(navigator)
            assertEquals(SessionNoteRoute, navigator.topKey)
            settle()
            CaptureSetupScopeManager.nav.navForward(navigator)

            assertEquals(TreeCaptureRoute(profilePicUrl = user.photoPath), navigator.topKey)
            coVerify { sessionTracker.startSession(user.id, "My org", "a note") }
        }

    @Test
    fun `a planter signed up from user select comes back and carries on with setup`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, SignupFlowRoute, clock = clock)

            // Sign-up goes back to user select with the new planter...
            navigator.popBackStackWithResult(SignupFlowRoute.NewUserId, user.id)
            // ...which reads it and starts setup with them.
            assertEquals(user.id, navigator.takeResult(SignupFlowRoute.NewUserId))
            settle()
            startCaptureSetup(user, navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
            assertEquals(user, CaptureSetupScopeManager.getData().user)
        }

    @Test
    fun `going back to user select and on again starts setup from the first step`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            settle()
            startCaptureSetup(user, navigator)
            settle()
            CaptureSetupScopeManager.nav.navForward(navigator)
            assertEquals(AddOrgRoute, navigator.topKey)

            settle()
            CaptureSetupScopeManager.nav.navToUserSelect(navigator)
            settle()
            startCaptureSetup(user, navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
        }
}