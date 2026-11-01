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
package org.greenstand.android.TreeTracker.uitest.viewmodel

import androidx.compose.runtime.CompositionLocalProvider
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavEntryContentKey
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.viewmodel.HandleUIEvents
import org.greenstand.android.TreeTracker.viewmodel.LocalSnackbarController
import org.greenstand.android.TreeTracker.viewmodel.NavigationEvent
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.greenstand.android.TreeTracker.viewmodel.ShowSnackbar
import org.greenstand.android.TreeTracker.viewmodel.SnackbarController
import org.greenstand.android.TreeTracker.viewmodel.TestEvent
import org.greenstand.android.TreeTracker.viewmodel.TestViewModel
import org.greenstand.android.TreeTracker.viewmodel.TextRef
import org.greenstand.android.TreeTracker.viewmodel.UiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleUIEventsUiTest : ComposeUiTest() {
    private val viewModel = TestViewModel()
    private val mockSnackbarController = mockk<SnackbarController>(relaxed = true)
    private val interceptedEvents = mutableListOf<UiEvent>()

    /** Renders [HandleUIEvents] with an optional hosting entry content key and [onEvent] handler. */
    private fun setEventHandler(
        entryContentKey: Any? = null,
        onEvent: ((UiEvent) -> Boolean)? = null,
    ) {
        setContent(inspectionMode = false) {
            CompositionLocalProvider(
                LocalNavEntryContentKey provides entryContentKey,
                LocalSnackbarController provides mockSnackbarController,
            ) {
                HandleUIEvents(viewModel, onEvent)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun recordAndReturn(handled: Boolean): (UiEvent) -> Boolean =
        { event ->
            interceptedEvents += event
            handled
        }

    // --- built-in dispatch ---

    @Test
    fun `navigation event runs on the navigator`() {
        viewModel.navigateTo(SettingsRoute)

        setEventHandler()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `navigation event with options is applied`() {
        backStack.add(SettingsRoute)
        backStack.add(MapRoute)
        viewModel.navigateWithOptions(UserSelectRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }

        setEventHandler()

        assertEquals(listOf(DashboardRoute, UserSelectRoute), backStack.toList())
    }

    @Test
    fun `custom navigation lambda runs with the navigator as receiver`() {
        backStack.add(SettingsRoute)
        backStack.add(MapRoute)
        viewModel.emit(NavigationEvent { popBackStackTo<DashboardRoute>() })

        setEventHandler()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `pop back stack event pops the top entry`() {
        backStack.add(SettingsRoute)
        viewModel.goBack()

        setEventHandler()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `show snackbar event is forwarded to the snackbar controller`() {
        val snackbar = ShowSnackbar(TextRef("Saved"))
        viewModel.emit(snackbar)

        setEventHandler()

        verify(exactly = 1) { mockSnackbarController.show(snackbar) }
    }

    @Test
    fun `events are handled in the order they were sent`() {
        viewModel.navigateTo(SettingsRoute)
        viewModel.navigateTo(MapRoute)

        setEventHandler()

        assertEquals(listOf(DashboardRoute, SettingsRoute, MapRoute), backStack.toList())
    }

    @Test
    fun `events sent after composition are handled`() {
        setEventHandler()

        composeTestRule.runOnIdle { viewModel.navigateTo(SettingsRoute) }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { backStack.size == 2 }

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `unhandled custom event is ignored and later events still run`() {
        viewModel.emit(TestEvent(1))
        viewModel.navigateTo(SettingsRoute)

        setEventHandler()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    // --- consumption ---

    @Test
    fun `already consumed event is skipped`() {
        viewModel.navigateTo(SettingsRoute)
        viewModel.events.replayCache
            .single()
            .getContentIfNotConsumed()

        setEventHandler(onEvent = recordAndReturn(handled = false))

        assertEquals(listOf(DashboardRoute), backStack.toList())
        assertTrue(interceptedEvents.isEmpty())
    }

    @Test
    fun `event is handled only once by two collectors`() {
        viewModel.navigateTo(SettingsRoute)

        setContent(inspectionMode = false) {
            HandleUIEvents(viewModel)
            HandleUIEvents(viewModel)
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    // --- onEvent interception ---

    @Test
    fun `onEvent returning true consumes the event`() {
        viewModel.navigateTo(SettingsRoute)

        setEventHandler(onEvent = recordAndReturn(handled = true))

        assertEquals(1, interceptedEvents.size)
        assertTrue(interceptedEvents.single() is NavigationEvent)
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `onEvent returning false falls through to the default handling`() {
        viewModel.navigateTo(SettingsRoute)

        setEventHandler(onEvent = recordAndReturn(handled = false))

        assertEquals(1, interceptedEvents.size)
        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `onEvent receives custom events`() {
        viewModel.emit(TestEvent(7))

        setEventHandler(onEvent = recordAndReturn(handled = true))

        assertEquals(listOf<UiEvent>(TestEvent(7)), interceptedEvents)
    }

    @Test
    fun `onEvent can intercept a pop back stack event`() {
        backStack.add(SettingsRoute)
        viewModel.goBack()

        setEventHandler(onEvent = recordAndReturn(handled = true))

        assertEquals(listOf<UiEvent>(PopBackStackEvent), interceptedEvents)
        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `onEvent can intercept a snackbar event`() {
        val snackbar = ShowSnackbar(TextRef("Saved"))
        viewModel.emit(snackbar)

        setEventHandler(onEvent = recordAndReturn(handled = true))

        assertEquals(listOf<UiEvent>(snackbar), interceptedEvents)
        verify(exactly = 0) { mockSnackbarController.show(any()) }
    }

    // --- hosting entry guard ---

    @Test
    fun `navigation runs when the hosting entry is on top`() {
        viewModel.navigateTo(SettingsRoute)

        setEventHandler(entryContentKey = DashboardRoute.toString())

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `navigation is dropped when the hosting entry is not on top`() {
        viewModel.navigateTo(MapRoute)

        setEventHandler(entryContentKey = SettingsRoute.toString())

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `pop back stack is dropped when the hosting entry is not on top`() {
        backStack.add(SettingsRoute)
        backStack.add(MapRoute)
        viewModel.goBack()

        setEventHandler(entryContentKey = SettingsRoute.toString())

        assertEquals(listOf(DashboardRoute, SettingsRoute, MapRoute), backStack.toList())
    }

    @Test
    fun `pop back stack runs when the hosting entry is on top`() {
        backStack.add(SettingsRoute)
        viewModel.goBack()

        setEventHandler(entryContentKey = SettingsRoute.toString())

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `snackbar is shown even when the hosting entry is not on top`() {
        val snackbar = ShowSnackbar(TextRef("Saved"))
        viewModel.emit(snackbar)

        setEventHandler(entryContentKey = SettingsRoute.toString())

        verify(exactly = 1) { mockSnackbarController.show(snackbar) }
    }

    @Test
    fun `dropped navigation is consumed and not replayed later`() {
        viewModel.navigateTo(MapRoute)
        setEventHandler(entryContentKey = SettingsRoute.toString())

        assertNull(
            viewModel.events.replayCache
                .single()
                .getContentIfNotConsumed(),
        )
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }
}