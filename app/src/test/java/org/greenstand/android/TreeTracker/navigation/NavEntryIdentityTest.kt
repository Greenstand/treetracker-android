/*
 * Copyright 2023 Treetracker
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

import android.app.Application
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs a real NavDisplay with the same entry decorators as Host, and checks when a screen
 * gets a new ViewModel. `navigate()` should create a new entry (as Navigation 2 did) even
 * when the same route was on the back stack; popping back should keep the existing entry.
 */
@RunWith(RobolectricTestRunner::class)
class NavEntryIdentityTest {
    // Unit tests here run without the merged manifest (includeAndroidResources is off), so
    // register the activity createComposeRule launches before it starts.
    @get:Rule(order = 0)
    val registerComposeActivity =
        object : TestWatcher() {
            override fun starting(description: Description) {
                val context = ApplicationProvider.getApplicationContext<Application>()
                shadowOf(context.packageManager).addOrUpdateActivity(
                    ActivityInfo().apply {
                        name = ComponentActivity::class.java.name
                        packageName = context.packageName
                    },
                )
            }
        }

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    /** The ViewModel instance number each route's screen last composed with. */
    private val viewModelInstances = mutableMapOf<NavKey, Int>()

    /** contentKeys of the entries NavDisplay currently has composed. */
    private val composedContentKeys = mutableSetOf<Any>()

    @Test
    fun `re-navigating to a route popped in the same navigation creates a new ViewModel`() {
        val navigator = showNavDisplay(DashboardRoute)
        navigate(navigator, UserSelectRoute)
        val firstUserSelect = viewModelInstances.getValue(UserSelectRoute)
        navigate(navigator, AddOrgRoute)
        navigate(navigator, TreeCaptureRoute(profilePicUrl = ""))

        // CaptureFlowNavigationController.goToUserSelect
        navigate(navigator, UserSelectRoute) {
            popUpTo<DashboardRoute> { inclusive = true }
            launchSingleTop = true
        }

        assertEquals(listOf<NavKey>(UserSelectRoute), navigator.backStack)
        assertNotEquals(firstUserSelect, viewModelInstances.getValue(UserSelectRoute))
    }

    @Test
    fun `re-navigating to a route with popUpTo itself inclusive creates a new ViewModel`() {
        val navigator = showNavDisplay(DashboardRoute)
        val firstDashboard = viewModelInstances.getValue(DashboardRoute)
        navigate(navigator, UserSelectRoute)

        // CaptureFlowNavigationController.goToDashboard
        navigate(navigator, DashboardRoute) {
            popUpTo<DashboardRoute> { inclusive = true }
            launchSingleTop = true
        }

        assertEquals(listOf<NavKey>(DashboardRoute), navigator.backStack)
        assertNotEquals(firstDashboard, viewModelInstances.getValue(DashboardRoute))
    }

    @Test
    fun `popping back to a route keeps its ViewModel`() {
        val navigator = showNavDisplay(DashboardRoute)
        navigate(navigator, UserSelectRoute)
        val userSelect = viewModelInstances.getValue(UserSelectRoute)
        navigate(navigator, AddOrgRoute)

        composeRule.runOnIdle { navigator.popBackStack() }
        composeRule.waitForIdle()

        assertEquals(userSelect, viewModelInstances.getValue(UserSelectRoute))
    }

    @Test
    fun `a navigation that leaves the stack unchanged keeps the displayed entry in sync with the navigator`() {
        val navigator = showNavDisplay(DashboardRoute)
        val dashboard = viewModelInstances.getValue(DashboardRoute)

        // NavDisplay doesn't rebuild entries for an unchanged stack; navigate() checks the
        // displayed contentKey still matches topContentKey
        navigate(navigator, DashboardRoute) { popUpTo<DashboardRoute> { inclusive = true } }

        assertEquals(listOf<NavKey>(DashboardRoute), navigator.backStack)
        assertEquals(dashboard, viewModelInstances.getValue(DashboardRoute))
    }

    @Test
    fun `launchSingleTop onto the same route keeps its ViewModel`() {
        val navigator = showNavDisplay(DashboardRoute)
        navigate(navigator, UserSelectRoute)
        val userSelect = viewModelInstances.getValue(UserSelectRoute)

        navigate(navigator, UserSelectRoute) { launchSingleTop = true }

        assertEquals(listOf(DashboardRoute, UserSelectRoute), navigator.backStack)
        assertEquals(userSelect, viewModelInstances.getValue(UserSelectRoute))
    }

    private fun showNavDisplay(startRoute: NavKey): Navigator {
        val navigator = Navigator(mutableStateListOf(startRoute))
        composeRule.setContent { TestNavDisplay(navigator) }
        composeRule.waitForIdle()
        assertDisplayedEntryIsTop(navigator)
        return navigator
    }

    private fun navigate(
        navigator: Navigator,
        route: NavKey,
        builder: NavOptions.() -> Unit = {},
    ) {
        composeRule.runOnIdle { navigator.navigate(route, builder) }
        composeRule.waitForIdle()
        assertDisplayedEntryIsTop(navigator)
    }

    /**
     * The entry NavDisplay shows must have the contentKey the Navigator reports for the top of the
     * stack: throttling and HandleUIEvents compare against topContentKey.
     */
    private fun assertDisplayedEntryIsTop(navigator: Navigator) {
        composeRule.runOnIdle { assertEquals(setOf(navigator.topContentKey), composedContentKeys) }
    }

    /**
     * Host.kt's back stack wiring and its saveable-state and ViewModel-store entry decorators, plus a
     * decorator that records which entries are composed. (Host's screen-tracking decorator needs Koin.)
     */
    @Composable
    private fun TestNavDisplay(navigator: Navigator) {
        val recordComposedKeys =
            remember {
                NavEntryDecorator<NavKey> { entry ->
                    DisposableEffect(entry.contentKey) {
                        composedContentKeys += entry.contentKey
                        onDispose { composedContentKeys -= entry.contentKey }
                    }
                    entry.Content()
                }
            }
        NavDisplay(
            backStack = navigator.backStack,
            onBack = { navigator.popBackStack() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                    recordComposedKeys,
                ),
            entryProvider =
                navigator.withEntryContentKeys(
                    entryProvider {
                        entry<DashboardRoute> { RecordViewModel(it) }
                        entry<UserSelectRoute> { RecordViewModel(it) }
                        entry<AddOrgRoute> { RecordViewModel(it) }
                        entry<TreeCaptureRoute> { RecordViewModel(it) }
                    },
                ),
        )
    }

    @Composable
    private fun RecordViewModel(route: NavKey) {
        val viewModel = viewModel<InstanceViewModel>()
        SideEffect { viewModelInstances[route] = viewModel.instance }
    }

    class InstanceViewModel : ViewModel() {
        val instance = nextInstance.incrementAndGet()
    }

    private companion object {
        val nextInstance = AtomicInteger()
    }
}