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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowData
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScope
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class CaptureFlowNavigationControllerTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val stepCounter = mockk<StepCounter>(relaxed = true)
    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val treeCapturer = mockk<TreeCapturer>(relaxed = true)
    private val deviceOrientation = mockk<DeviceOrientation>(relaxed = true)

    private val clock = FakeClock()
    private val capture = TreeCaptureRoute(profilePicUrl = "/photos/ada.jpg")

    private val standardFlow =
        destinations("capture/{profilePicUrl}", "tree-image-review/{photoPath}", "tree-height-selection")

    @Before
    fun setUp() {
        CaptureFlowScopeManager.close()
        stopKoin()
        startKoin {
            modules(
                module {
                    scope<CaptureFlowScope> {
                        scoped { CaptureFlowData() }
                    }
                },
            )
        }
        CaptureFlowScopeManager.open()

        val tree = mockk<Tree>()
        every { tree.photoPath } returns "/trees/1.jpg"
        every { treeCapturer.currentTree } returns tree
        coEvery { treeCapturer.saveTree() } returns true
    }

    @After
    fun tearDown() {
        CaptureFlowScopeManager.close()
        stopKoin()
    }

    private fun controllerFor(captureFlow: List<Destination>) =
        CaptureFlowNavigationController(
            orgRepo = orgRepoWith(captureFlow = captureFlow),
            stepCounter = stepCounter,
            sessionTracker = sessionTracker,
            locationDataCapturer = locationDataCapturer,
            treeCapturer = treeCapturer,
            deviceOrientation = deviceOrientation,
        )

    /** Moves the fake clock past the unscoped throttle window so the next throttled call goes through. */
    private fun settle() = clock.advance(THROTTLE_WINDOW_MS)

    private fun verifySessionEnded() {
        verify(timeout = SESSION_END_TIMEOUT_MS) { locationDataCapturer.turnOffTreeCaptureMode() }
        coVerifyOrder {
            sessionTracker.endSession()
            stepCounter.disable()
            deviceOrientation.disable()
            locationDataCapturer.stopGpsUpdates()
            locationDataCapturer.turnOffTreeCaptureMode()
        }
    }

    // --- navForward through the flow ---

    @Test
    fun `navForward from capture opens image review with the current tree photo`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, capture, TreeImageReviewRoute(photoPath = "/trees/1.jpg")),
                navigator.backStack,
            )
            assertEquals(Destination("tree-image-review/{photoPath}"), controller.currentDestination)
        }

    @Test
    fun `navForward to image review without a current tree uses an empty photo path`() =
        runTest {
            every { treeCapturer.currentTree } returns null
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            controller.navForward(navigator)

            assertEquals(TreeImageReviewRoute(photoPath = ""), navigator.topKey)
        }

    @Test
    fun `navForward resolves the image-review alias`() =
        runTest {
            val controller = controllerFor(destinations("tree-capture", "image-review"))
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            controller.navForward(navigator)

            assertEquals(TreeImageReviewRoute(photoPath = "/trees/1.jpg"), navigator.topKey)
        }

    @Test
    fun `navForward resolves the legacy capture route mid-flow`() =
        runTest {
            val controller = controllerFor(destinations("tree-height-selection", "capture/{profilePicUrl}"))
            val navigator = fakeNavigator(DashboardRoute, TreeHeightScreenRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(TreeCaptureRoute(profilePicUrl = ""), navigator.topKey)
        }

    @Test
    fun `navForward resolves the tree-capture alias mid-flow`() =
        runTest {
            val controller = controllerFor(destinations("tree-height-selection", "tree-capture"))
            val navigator = fakeNavigator(DashboardRoute, TreeHeightScreenRoute, clock = clock)

            controller.navForward(navigator)

            assertEquals(TreeCaptureRoute(profilePicUrl = ""), navigator.topKey)
        }

    @Test
    fun `navForward resolves no-arg routes in the capture flow`() =
        runTest {
            val controller = controllerFor(destinations("tree-capture", "session-note"))
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            controller.navForward(navigator)

            assertEquals(SessionNoteRoute, navigator.topKey)
        }

    @Test
    fun `navForward walks every step of the capture flow in order`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, capture, TreeImageReviewRoute("/trees/1.jpg"), TreeHeightScreenRoute),
                navigator.backStack,
            )
            assertTrue(controller.isAtEnd)
            coVerify(exactly = 0) { treeCapturer.saveTree() }
        }

    @Test
    fun `navForward to an unknown destination throws and leaves the stack untouched`() =
        runTest {
            val controller = controllerFor(destinations("tree-capture", "bogus-route"))
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            val error = runCatching { controller.navForward(navigator) }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertEquals("Unknown capture flow destination: bogus-route", error?.message)
            assertEquals(listOf(DashboardRoute, capture), navigator.backStack)
        }

    // --- end of capture flow ---

    @Test
    fun `navForward past the last step saves the tree and pops back to capture`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()

            controller.navForward(navigator)

            coVerify(exactly = 1) { treeCapturer.saveTree() }
            assertEquals(listOf(DashboardRoute, capture), navigator.backStack)
        }

    @Test
    fun `navForward past the last step resets the flow to the first destination`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            controller.navForward(navigator)

            assertEquals(Destination("capture/{profilePicUrl}"), controller.currentDestination)
            assertTrue(controller.isAtStart)
        }

    @Test
    fun `navForward past the last step pops back to capture even when saving fails`() =
        runTest {
            coEvery { treeCapturer.saveTree() } returns false
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            controller.navForward(navigator)

            coVerify(exactly = 1) { treeCapturer.saveTree() }
            assertEquals(listOf(DashboardRoute, capture), navigator.backStack)
            assertTrue(controller.isAtStart)
        }

    @Test
    fun `navForward past the last step saves the tree before leaving the last screen`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            var topWhenSaved: Any? = null
            coEvery { treeCapturer.saveTree() } coAnswers {
                topWhenSaved = navigator.topKey
                true
            }

            controller.navForward(navigator)

            assertEquals(TreeHeightScreenRoute, topWhenSaved)
        }

    @Test
    fun `navForward past the last step is not throttled`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            // No settle(): the loop back uses the non-throttled popBackStackTo.
            controller.navForward(navigator)

            assertEquals(listOf(DashboardRoute, capture), navigator.backStack)
        }

    @Test
    fun `navForward after a completed capture starts the next tree at image review`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)
            settle()

            controller.navForward(navigator)

            assertEquals(
                listOf(DashboardRoute, capture, TreeImageReviewRoute("/trees/1.jpg")),
                navigator.backStack,
            )
        }

    @Test
    fun `completing a capture keeps the session and the capture scope open`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()
            controller.navForward(navigator)

            controller.navForward(navigator)

            coVerify(exactly = 0) { sessionTracker.endSession() }
            assertTrue(CaptureFlowScopeManager.isOpen)
        }

    @Test
    fun `navForward on a single-step flow saves the tree and stays on capture`() =
        runTest {
            val controller = controllerFor(destinations("tree-capture"))
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

            controller.navForward(navigator)

            coVerify(exactly = 1) { treeCapturer.saveTree() }
            assertEquals(listOf(DashboardRoute, capture), navigator.backStack)
        }

    // --- navBackward ---

    @Test
    fun `navBackward from the middle pops to the previous step`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()

            controller.navBackward(navigator)

            assertEquals(listOf(DashboardRoute, capture), navigator.backStack)
            assertEquals(Destination("capture/{profilePicUrl}"), controller.currentDestination)
            assertTrue(controller.isAtStart)
        }

    @Test
    fun `navBackward from the middle keeps the session running`() =
        runTest {
            val controller = controllerFor(standardFlow)
            val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)
            controller.navForward(navigator)
            settle()

            controller.navBackward(navigator)

            coVerify(exactly = 0) { sessionTracker.endSession() }
            assertTrue(CaptureFlowScopeManager.isOpen)
        }

    @Test
    fun `navBackward at the start goes to the dashboard`() {
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, capture, clock = clock)

        controller.navBackward(navigator)

        assertEquals(listOf<Any>(DashboardRoute), navigator.backStack)
    }

    @Test
    fun `navBackward at the start ends the session and closes the capture scope`() {
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

        controller.navBackward(navigator)

        assertFalse(CaptureFlowScopeManager.isOpen)
        verifySessionEnded()
    }

    // --- goToDashboard ---

    @Test
    fun `goToDashboard clears the stack down to a single dashboard`() {
        val controller = controllerFor(standardFlow)
        val navigator =
            fakeNavigator(
                DashboardRoute,
                UserSelectRoute,
                capture,
                TreeImageReviewRoute("/trees/1.jpg"),
                clock = clock,
            )

        controller.goToDashboard(navigator)

        assertEquals(listOf<Any>(DashboardRoute), navigator.backStack)
    }

    @Test
    fun `goToDashboard without dashboard on the stack pushes it`() {
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(capture, clock = clock)

        controller.goToDashboard(navigator)

        assertEquals(listOf(capture, DashboardRoute), navigator.backStack)
    }

    @Test
    fun `goToDashboard closes the capture scope immediately`() {
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

        controller.goToDashboard(navigator)

        assertFalse(CaptureFlowScopeManager.isOpen)
    }

    @Test
    fun `goToDashboard ends the session and stops sensors and GPS in order`() {
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

        controller.goToDashboard(navigator)

        verifySessionEnded()
    }

    @Test
    fun `goToDashboard still navigates when ending the session fails`() {
        coEvery { sessionTracker.endSession() } throws IllegalStateException("db closed")
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

        controller.goToDashboard(navigator)

        assertEquals(listOf<Any>(DashboardRoute), navigator.backStack)
        coVerify(timeout = SESSION_END_TIMEOUT_MS) { sessionTracker.endSession() }
        verify(exactly = 0) { stepCounter.disable() }
        verify(exactly = 0) { locationDataCapturer.stopGpsUpdates() }
    }

    // --- goToUserSelect ---

    @Test
    fun `goToUserSelect replaces the whole stack including dashboard with user select`() {
        val controller = controllerFor(standardFlow)
        val navigator =
            fakeNavigator(
                DashboardRoute,
                UserSelectRoute,
                capture,
                TreeImageReviewRoute("/trees/1.jpg"),
                clock = clock,
            )

        controller.goToUserSelect(navigator)

        assertEquals(listOf<Any>(UserSelectRoute), navigator.backStack)
    }

    @Test
    fun `goToUserSelect ends the session and closes the capture scope`() {
        val controller = controllerFor(standardFlow)
        val navigator = fakeNavigator(DashboardRoute, capture, clock = clock)

        controller.goToUserSelect(navigator)

        assertFalse(CaptureFlowScopeManager.isOpen)
        verifySessionEnded()
    }

    private companion object {
        const val SESSION_END_TIMEOUT_MS = 5_000L
    }
}