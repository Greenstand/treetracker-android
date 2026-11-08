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
package org.greenstand.android.TreeTracker.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseViewModelTest {
    private val viewModel = TestViewModel()

    private fun replayedEvents(): List<UiEvent?> = viewModel.events.replayCache.map { it.getContentIfNotConsumed() }

    private fun singleReplayedEvent(): UiEvent? =
        viewModel.events.replayCache
            .single()
            .getContentIfNotConsumed()

    // --- state ---

    @Test
    fun `state starts with the initial state`() {
        assertEquals(TestState(), viewModel.state.value)
    }

    @Test
    fun `updateState applies the transform to the current state`() {
        viewModel.handleAction(TestAction.Increment)
        viewModel.handleAction(TestAction.Increment)

        assertEquals(TestState(count = 2), viewModel.state.value)
    }

    @Test
    fun `updateState keeps fields the transform does not touch`() {
        viewModel.handleAction(TestAction.Increment)

        viewModel.handleAction(TestAction.Rename("renamed"))

        assertEquals(TestState(count = 1, label = "renamed"), viewModel.state.value)
    }

    @Test
    fun `currentState reflects the latest update`() {
        viewModel.handleAction(TestAction.Rename("latest"))

        assertEquals(viewModel.state.value, viewModel.readCurrentState())
        assertEquals("latest", viewModel.readCurrentState().label)
    }

    @Test
    fun `state collectors see every distinct update`() =
        runTest {
            viewModel.state.test {
                assertEquals(TestState(), awaitItem())
                viewModel.handleAction(TestAction.Increment)
                assertEquals(TestState(count = 1), awaitItem())
                viewModel.handleAction(TestAction.Rename("a"))
                assertEquals(TestState(count = 1, label = "a"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // --- events ---

    @Test
    fun `no events are emitted initially`() {
        assertTrue(viewModel.events.replayCache.isEmpty())
    }

    @Test
    fun `sendEvent emits the event wrapped in a ConsumableEvent`() {
        viewModel.emit(TestEvent(1))

        assertEquals(TestEvent(1), singleReplayedEvent())
    }

    @Test
    fun `triggerEvent is an alias for sendEvent`() {
        viewModel.emitViaTrigger(TestEvent(2))

        assertEquals(TestEvent(2), singleReplayedEvent())
    }

    @Test
    fun `popBackStack emits a PopBackStackEvent`() {
        viewModel.goBack()

        assertSame(PopBackStackEvent, singleReplayedEvent())
    }

    @Test
    fun `navigate emits a navigation event that pushes the route`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute)
            viewModel.navigateTo(SettingsRoute)

            val event = singleReplayedEvent() as NavigationEvent
            event.navigate(navigator)

            assertEquals(listOf(DashboardRoute, SettingsRoute), navigator.backStack)
        }

    @Test
    fun `navigate with options applies popUpTo and launchSingleTop`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, UserSelectRoute, WalletSelectRoute)
            viewModel.navigateWithOptions(UserSelectRoute) {
                popUpTo<DashboardRoute>()
                launchSingleTop = true
            }

            val event = singleReplayedEvent() as NavigationEvent
            event.navigate(navigator)

            assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
        }

    @Test
    fun `navigate with an inclusive popUpTo removes the target too`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, SettingsRoute)
            viewModel.navigateWithOptions(MapRoute) {
                popUpTo<DashboardRoute> { inclusive = true }
            }

            val event = singleReplayedEvent() as NavigationEvent
            event.navigate(navigator)

            assertEquals(listOf<Any>(MapRoute), navigator.backStack)
        }

    @Test
    fun `navigate events are throttled`() =
        runTest {
            val clock = FakeClock()
            val navigator = fakeNavigator(DashboardRoute, clock = clock)
            // A navigation right now opens the throttle window.
            navigator.navigate(SettingsRoute)
            viewModel.navigateTo(MapRoute)

            val event = singleReplayedEvent() as NavigationEvent
            event.navigate(navigator)

            assertEquals(listOf(DashboardRoute, SettingsRoute), navigator.backStack)
        }

    @Test
    fun `navigate events with options are throttled`() =
        runTest {
            val clock = FakeClock()
            val navigator = fakeNavigator(DashboardRoute, clock = clock)
            navigator.navigate(SettingsRoute)
            viewModel.navigateWithOptions(MapRoute) { launchSingleTop = true }

            val event = singleReplayedEvent() as NavigationEvent
            event.navigate(navigator)

            assertEquals(listOf(DashboardRoute, SettingsRoute), navigator.backStack)
        }

    @Test
    fun `events are delivered in the order they were sent`() {
        viewModel.emit(TestEvent(1))
        viewModel.goBack()
        viewModel.emit(TestEvent(2))

        assertEquals(listOf(TestEvent(1), PopBackStackEvent, TestEvent(2)), replayedEvents())
    }

    @Test
    fun `replay keeps only the five most recent events`() {
        (1..7).forEach { viewModel.emit(TestEvent(it)) }

        assertEquals((3..7).map { TestEvent(it) }, replayedEvents())
    }

    @Test
    fun `a late subscriber receives events sent before it subscribed`() =
        runTest {
            viewModel.emit(TestEvent(1))
            viewModel.emit(TestEvent(2))

            viewModel.events.test {
                assertEquals(TestEvent(1), awaitItem().getContentIfNotConsumed())
                assertEquals(TestEvent(2), awaitItem().getContentIfNotConsumed())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `an active subscriber receives events as they are sent`() =
        runTest {
            viewModel.events.test {
                viewModel.emit(TestEvent(9))

                assertEquals(TestEvent(9), awaitItem().getContentIfNotConsumed())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `an event consumed by one subscriber is empty for the next`() =
        runTest {
            viewModel.emit(TestEvent(1))

            viewModel.events.test {
                assertEquals(TestEvent(1), awaitItem().getContentIfNotConsumed())
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.events.test {
                assertNull(awaitItem().getContentIfNotConsumed())
                cancelAndIgnoreRemainingEvents()
            }
        }
}