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

import androidx.compose.material.SnackbarDuration
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class UiEventsTest {
    // --- ConsumableEvent ---

    @Test
    fun `ConsumableEvent returns its content on the first read`() {
        val event = TestEvent(1)

        assertSame(event, ConsumableEvent(event).getContentIfNotConsumed())
    }

    @Test
    fun `ConsumableEvent returns null on every later read`() {
        val consumable = ConsumableEvent(TestEvent(1))
        consumable.getContentIfNotConsumed()

        assertNull(consumable.getContentIfNotConsumed())
        assertNull(consumable.getContentIfNotConsumed())
    }

    @Test
    fun `separate ConsumableEvents wrapping the same event are consumed independently`() {
        val event = TestEvent(1)
        val first = ConsumableEvent(event)
        val second = ConsumableEvent(event)
        first.getContentIfNotConsumed()

        assertSame(event, second.getContentIfNotConsumed())
    }

    @Test
    fun `ConsumableEvent hands its content to exactly one of many concurrent readers`() {
        val consumable = ConsumableEvent(TestEvent(1))
        val executor = Executors.newFixedThreadPool(THREADS)
        try {
            val reads = List(THREADS) { Callable { consumable.getContentIfNotConsumed() } }

            val results = executor.invokeAll(reads).map { it.get() }

            assertEquals(1, results.count { it != null })
        } finally {
            executor.shutdownNow()
        }
    }

    // --- NavigationEvent ---

    @Test
    fun `NavigationEvent runs its lambda with the navigator as receiver`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute)
            val event = NavigationEvent { navigate(SettingsRoute) }

            event.navigate(navigator)

            assertEquals(listOf(DashboardRoute, SettingsRoute), navigator.backStack)
        }

    @Test
    fun `NavigationEvent supports suspend calls in its lambda`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, SettingsRoute)
            val event =
                NavigationEvent {
                    yield()
                    popBackStack()
                }

            event.navigate(navigator)

            assertEquals(listOf<Any>(DashboardRoute), navigator.backStack)
        }

    // --- PopBackStackEvent ---

    @Test
    fun `PopBackStackEvent is a singleton with a readable name`() {
        assertEquals("PopBackStackEvent", PopBackStackEvent.toString())
    }

    // --- ShowSnackbar ---

    @Test
    fun `ShowSnackbar defaults to a short snackbar without an action`() {
        val snackbar = ShowSnackbar(TextRef("Saved"))

        assertEquals(TextRef.Plain("Saved"), snackbar.message)
        assertEquals(SnackbarDuration.Short, snackbar.duration)
        assertNull(snackbar.actionLabel)
        assertNull(snackbar.onAction)
    }

    @Test
    fun `ShowSnackbar keeps an explicit duration and action`() {
        val onAction = {}
        val snackbar =
            ShowSnackbar(
                message = TextRef("Deleted"),
                duration = SnackbarDuration.Long,
                actionLabel = TextRef("Undo"),
                onAction = onAction,
            )

        assertEquals(SnackbarDuration.Long, snackbar.duration)
        assertEquals(TextRef.Plain("Undo"), snackbar.actionLabel)
        assertSame(onAction, snackbar.onAction)
    }

    @Test
    fun `ShowSnackbar instances with the same values are equal`() {
        assertEquals(ShowSnackbar(TextRef("Saved")), ShowSnackbar(TextRef("Saved")))
        assertNotEquals(ShowSnackbar(TextRef("Saved")), ShowSnackbar(TextRef("Deleted")))
        assertNotEquals(
            ShowSnackbar(TextRef("Saved")),
            ShowSnackbar(TextRef("Saved"), duration = SnackbarDuration.Indefinite),
        )
    }

    private companion object {
        const val THREADS = 8
    }
}