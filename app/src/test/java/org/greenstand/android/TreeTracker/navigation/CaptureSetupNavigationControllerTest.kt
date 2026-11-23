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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
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

    private fun settle() = clock.advance(THROTTLE_WINDOW_MS)

    private val fullFlow = destinations("user-select", "wallet-select", "add-org", "session-note")

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
    fun `navForward on the last step keeps the setup scope open`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)

            assertTrue(CaptureSetupScopeManager.isOpen)
        }

    @Test
    fun `navForward again after capture pops back onto the last step reopens capture`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "add-org"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)

            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()
            navigator.throttledPopBackStack()
            settle()
            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, UserSelectRoute, AddOrgRoute, TreeCaptureRoute("/photos/ada.jpg")),
                navigator.backStack,
            )
            coVerify(exactly = 2) { sessionTracker.startSession() }
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

    @Test
    fun `second navForward from user select after it navigated is dropped`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "add-org"))
            val navigator =
                fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
                    .scopedTo(UserSelectRoute.toString())

            controller.navForward(navigator)
            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, AddOrgRoute), navigator.backStack)
            assertEquals(Destination("add-org"), controller.currentDestination)
            coVerify(exactly = 0) { sessionTracker.startSession() }
            assertTrue(CaptureSetupScopeManager.isOpen)
        }

    @Test
    fun `navForward is dropped while an earlier call is still running`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator =
                fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
                    .scopedTo(UserSelectRoute.toString())
            val sessionStarted = CompletableDeferred<Unit>()
            coEvery { sessionTracker.startSession() } coAnswers { sessionStarted.await() }

            launch { controller.navForward(navigator) }
            launch { controller.navForward(navigator) }
            advanceUntilIdle()
            sessionStarted.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { sessionTracker.startSession() }
            assertEquals(
                listOf(DashboardRoute, UserSelectRoute, TreeCaptureRoute("/photos/ada.jpg")),
                navigator.backStack,
            )
        }

    @Test
    fun `navForward runs again after an earlier call failed`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator =
                fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
                    .scopedTo(UserSelectRoute.toString())
            coEvery { sessionTracker.startSession() } throws IllegalStateException("db") andThen Unit

            runCatching { controller.navForward(navigator) }
            controller.navForward(navigator)

            coVerify(exactly = 2) { sessionTracker.startSession() }
            assertEquals(TreeCaptureRoute("/photos/ada.jpg"), navigator.topKey)
        }

    @Test
    fun `navForward from a screen that is no longer on top does nothing`() =
        runTest {
            val controller = controllerFor(destinations("user-select"))
            val navigator =
                fakeNavigator(DashboardRoute, UserSelectRoute, AddOrgRoute, clock = clock)
                    .scopedTo(UserSelectRoute.toString())

            controller.navForward(navigator)

            coVerify(exactly = 0) { sessionTracker.startSession() }
            assertEquals(listOf(DashboardRoute, UserSelectRoute, AddOrgRoute), navigator.backStack)
            assertTrue(CaptureSetupScopeManager.isOpen)
        }

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
    fun `a fresh controller under a restored Add org continues from Add org (#1418)`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddOrgRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(SessionNoteRoute, navigator.backStack.last())
            assertEquals(Destination("session-note"), controller.currentDestination)
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

    @Test
    fun `navForward after system back from add org opens add org again`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "add-org"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            navigator.popBackStack()
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, AddOrgRoute), navigator.backStack)
            coVerify(exactly = 0) { sessionTracker.startSession() }
            assertTrue(CaptureSetupScopeManager.isOpen)
        }

    @Test
    fun `navForward after system back in the middle of a flow re-enters the popped step`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()
            navigator.popBackStack()
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddOrgRoute), navigator.backStack)
            assertEquals(Destination("add-org"), controller.currentDestination)
        }

    @Test
    fun `navForward from a screen outside the flow continues from the step beneath it`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddWalletRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(Destination("add-org"), controller.currentDestination)
            assertEquals(AddOrgRoute, navigator.topKey)
        }

    @Test
    fun `navForward after a double tap on back from add org opens add org again`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "add-org"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navBackward(navigator)
            controller.navBackward(navigator)
            assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, AddOrgRoute), navigator.backStack)
            assertEquals(Destination("add-org"), controller.currentDestination)
        }

    @Test
    fun `navFromNewUserCreation after system back re-enters the popped step`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "wallet-select", "add-org"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            navigator.popBackStack()
            settle()
            navigator.navigate(SignupFlowRoute)
            settle()

            controller.navFromNewUserCreation(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute), navigator.backStack)
            assertEquals(Destination("wallet-select"), controller.currentDestination)
        }

    @Test
    fun `navForward after back from add wallet continues from wallet select`() =
        runTest {
            val controller = controllerFor(fullFlow)
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            navigator.navigate(AddWalletRoute)
            settle()
            controller.navBackward(navigator)
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddOrgRoute), navigator.backStack)
        }

    @Test
    fun `navForward through a flow with a repeated step reaches the end`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "session-note", "add-org", "session-note"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            repeat(3) {
                controller.navForward(navigator)
                settle()
            }

            controller.navForward(navigator)

            assertEquals(TreeCaptureRoute(profilePicUrl = "/photos/ada.jpg"), navigator.topKey)
            coVerify(exactly = 1) { sessionTracker.startSession() }
        }

    @Test
    fun `navForward after system back to a repeated step re-enters the step after it`() =
        runTest {
            val controller = controllerFor(destinations("user-select", "session-note", "add-org", "session-note"))
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()
            navigator.popBackStack()
            settle()

            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute, SessionNoteRoute, AddOrgRoute), navigator.backStack)
            assertEquals(Destination("add-org"), controller.currentDestination)
        }

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
            assertTrue(CaptureSetupScopeManager.isOpen)
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