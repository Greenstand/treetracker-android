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
package org.greenstand.android.TreeTracker.navigation

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScope
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class CaptureSetupNavigationControllerTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val stepCounter = mockk<StepCounter>(relaxed = true)
    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val deviceOrientation = mockk<DeviceOrientation>(relaxed = true)
    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)

    private val clock = FakeClock()

    private val user =
        User(
            id = 1L,
            wallet = "planter-wallet",
            numberOfTrees = 0,
            firstName = "Ada",
            lastName = "Planter",
            photoPath = "/photos/ada.jpg",
            isPowerUser = false,
            unreadMessagesAvailable = false,
        )

    @Before
    fun setUp() {
        CaptureSetupScopeManager.close()
        stopKoin()
        startKoin {
            modules(
                module {
                    single { exceptionDataCollector }
                    scope<CaptureSetupScope> {
                        scoped { CaptureSetupData(get()) }
                    }
                },
            )
        }
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().user = user
    }

    @After
    fun tearDown() {
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    private fun controllerFor(setupFlow: List<Destination>) =
        CaptureSetupNavigationController(
            orgRepo = orgRepoWith(setupFlow = setupFlow),
            stepCounter = stepCounter,
            sessionTracker = sessionTracker,
            locationDataCapturer = locationDataCapturer,
            deviceOrientation = deviceOrientation,
        )

    /** Moves the fake clock past the unscoped throttle window so the next throttled call goes through. */
    private fun settle() = clock.advance(THROTTLE_WINDOW_MS)

    private val fullFlow = destinations("user-select", "wallet-select", "add-org", "session-note")

    // --- navForward through the flow ---

    @Test
    fun `navForward from the first step navigates to the second destination`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
            assertEquals(Destination("wallet-select"), controller.currentDestination)
            assertFalse(controller.isAtStart)
            assertFalse(controller.isAtEnd)
        }

    @Test
    fun `navForward walks every step of a multi-step flow in order`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddOrgRoute, SessionNoteRoute),
                navigator.backStack,
            )
            assertEquals(Destination("session-note"), controller.currentDestination)
            assertTrue(controller.isAtEnd)
        }

    @Test
    fun `navForward before the end does not start a session`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            coVerify(exactly = 0) { sessionTracker.startSession() }
            verify(exactly = 0) { stepCounter.enable() }
            verify(exactly = 0) { deviceOrientation.enable() }
            verify(exactly = 0) { locationDataCapturer.startGpsUpdates() }
            assertTrue(CaptureSetupScopeManager.isOpen)
        }

    @Test
    fun `navForward resolves tree height in a setup flow`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "tree-height-selection"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(TreeHeightScreenRoute, navigator.topKey)
        }

    @Test
    fun `navForward to an unknown destination throws and leaves the stack untouched`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "bogus-route"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            val error = runCatching { controller.navForward(navigator) }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Unknown setup flow destination: bogus-route", error?.message)
            assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
        }

    @Test
    fun `navForward to an arg route in the setup flow throws`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "tree-capture"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            val error = runCatching { controller.navForward(navigator) }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Unknown setup flow destination: tree-capture", error?.message)
        }

    // --- end of setup flow ---

    @Test
    fun `navForward on the last step navigates to tree capture with the user photo`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, UserSelectRoute, TreeCaptureRoute(profilePicUrl = "/photos/ada.jpg")),
                navigator.backStack,
            )
        }

    @Test
    fun `navForward on the last step starts sensors, session and GPS in order`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            coVerifyOrder {
                stepCounter.enable()
                deviceOrientation.enable()
                sessionTracker.startSession()
                locationDataCapturer.startGpsUpdates()
            }
        }

    @Test
    fun `navForward on the last step starts the session before navigating`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            var topWhenSessionStarted: Any? = null
            coEvery { sessionTracker.startSession() } coAnswers { topWhenSessionStarted = navigator.topKey }

            controller.navForward(navigator)

            assertEquals(UserSelectRoute, topWhenSessionStarted)
            assertTrue(navigator.topKey is TreeCaptureRoute)
        }

    @Test
    fun `navForward on the last step closes the setup scope`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            assertFalse(CaptureSetupScopeManager.isOpen)
        }

    @Test
    fun `navForward on the last step without a selected user uses an empty photo path`() =
        runTest {
            CaptureSetupScopeManager.getData().user = null
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(TreeCaptureRoute(profilePicUrl = ""), navigator.topKey)
        }

    @Test
    fun `navForward after walking a multi-step flow finishes setup`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "session-note"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, UserSelectRoute, SessionNoteRoute, TreeCaptureRoute("/photos/ada.jpg")),
                navigator.backStack,
            )
            coVerify(exactly = 1) { sessionTracker.startSession() }
        }

    @Test
    fun `navForward on an empty setup flow finishes setup immediately`() =
        runTest {
            val controller = controllerFor(emptyList())
            val navigator = fakeNavigator(DashboardRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, TreeCaptureRoute("/photos/ada.jpg")), navigator.backStack)
            coVerify { sessionTracker.startSession() }
        }

    // --- navBackward ---

    @Test
    fun `navBackward from the middle pops and returns to the previous step`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()

            controller.navBackward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
            assertEquals(Destination("user-select"), controller.currentDestination)
            assertTrue(controller.isAtStart)
        }

    @Test
    fun `navForward after navBackward re-enters the same next step`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navBackward(navigator)
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
        }

    @Test
    fun `navBackward at the start pops the first screen`() {
        val controller = controllerFor(fullFlow)
        val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

        controller.navBackward(navigator)

        assertEquals(listOf<Any>(DashboardRoute), navigator.backStack)
        assertNull(controller.currentDestination)
        assertTrue(controller.isAtStart)
    }

    @Test
    fun `navBackward does not touch the session`() {
        val controller = controllerFor(fullFlow)
        val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

        controller.navBackward(navigator)

        coVerify(exactly = 0) { sessionTracker.startSession() }
        coVerify(exactly = 0) { sessionTracker.endSession() }
        assertTrue(CaptureSetupScopeManager.isOpen)
    }

    // --- navToUserSelect ---

    @Test
    fun `navToUserSelect pops back to dashboard and shows user select`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()

            controller.navToUserSelect(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
        }

    @Test
    fun `navToUserSelect resets the flow to the first step`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()

            controller.navToUserSelect(navigator)

            assertEquals(Destination("user-select"), controller.currentDestination)
            assertTrue(controller.isAtStart)
        }

    @Test
    fun `navForward after navToUserSelect restarts the flow at the second step`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()
            controller.navToUserSelect(navigator)
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
        }

    @Test
    fun `navToUserSelect without dashboard on the stack still navigates`() {
        val controller = controllerFor(fullFlow)
        val navigator = fakeNavigator(SettingsRoute, clock = clock)

        controller.navToUserSelect(navigator)

        assertEquals(listOf(SettingsRoute, UserSelectRoute), navigator.backStack)
    }

    // --- navFromNewUserCreation ---

    @Test
    fun `navFromNewUserCreation replaces signup with the next setup step`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "wallet-select", "session-note"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, SignupFlowRoute, clock = clock)

            controller.navFromNewUserCreation(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
            assertEquals(Destination("wallet-select"), controller.currentDestination)
            coVerify(exactly = 0) { sessionTracker.startSession() }
        }

    @Test
    fun `navFromNewUserCreation then navForward continues the flow`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "wallet-select", "session-note"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, SignupFlowRoute, clock = clock)
            controller.navFromNewUserCreation(navigator)
            settle()

            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, SessionNoteRoute),
                navigator.backStack,
            )
        }

    @Test
    fun `navFromNewUserCreation when signup was the last step finishes setup`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, SignupFlowRoute, clock = clock)

            controller.navFromNewUserCreation(navigator)

            assertEquals(TreeCaptureRoute(profilePicUrl = "/photos/ada.jpg"), navigator.topKey)
            assertTrue(controller.isAtEnd)
            coVerify { sessionTracker.startSession() }
            verify { stepCounter.enable() }
            verify { deviceOrientation.enable() }
            verify { locationDataCapturer.startGpsUpdates() }
            assertFalse(CaptureSetupScopeManager.isOpen)
        }

    @Test
    fun `navFromNewUserCreation to an unknown destination throws`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "bogus-route"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, SignupFlowRoute, clock = clock)

            val error = runCatching { controller.navFromNewUserCreation(navigator) }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Unknown setup flow destination: bogus-route", error?.message)
            assertEquals(listOf(DashboardRoute, UserSelectRoute, SignupFlowRoute), navigator.backStack)
        }

    // --- throttling ---

    @Test
    fun `a second navForward inside the throttle window does not navigate`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
        }
}