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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    private val clock = FakeClock()
    private val navigator = fakeNavigator(SplashRoute(), clock = clock)

    private fun navigateSettled(
        route: androidx.navigation3.runtime.NavKey,
        builder: NavOptions.() -> Unit = {},
    ) {
        clock.advance(300)
        navigator.navigate(route, builder)
    }

    @Test
    fun `navigate pushes onto the back stack`() {
        navigateSettled(DashboardRoute)
        navigateSettled(SettingsRoute)

        assertEquals(listOf(SplashRoute(), DashboardRoute, SettingsRoute), navigator.backStack)
    }

    @Test
    fun `popUpTo non-inclusive pops above target`() {
        navigateSettled(DashboardRoute)
        navigateSettled(UserSelectRoute)
        navigateSettled(WalletSelectRoute)

        navigateSettled(TreeCaptureRoute("pic")) { popUpTo<DashboardRoute>() }

        assertEquals(listOf(SplashRoute(), DashboardRoute, TreeCaptureRoute("pic")), navigator.backStack)
    }

    @Test
    fun `popUpTo inclusive pops target as well`() {
        navigateSettled(LanguageRoute(isFromTopBar = false))

        navigateSettled(DashboardRoute) {
            popUpTo<SplashRoute> { inclusive = true }
            launchSingleTop = true
        }

        assertEquals(listOf(DashboardRoute), navigator.backStack)
    }

    @Test
    fun `popUpTo with absent target pops nothing but still navigates`() {
        navigateSettled(DashboardRoute)

        navigateSettled(SettingsRoute) { popUpTo<MapRoute> { inclusive = true } }

        assertEquals(listOf(SplashRoute(), DashboardRoute, SettingsRoute), navigator.backStack)
    }

    @Test
    fun `popUpToRoot clears the stack before navigating`() {
        navigateSettled(DashboardRoute)
        navigateSettled(SettingsRoute)

        navigateSettled(SignupFlowRoute) {
            popUpToRoot()
            launchSingleTop = true
        }

        assertEquals(listOf<Any>(SignupFlowRoute), navigator.backStack)
    }

    @Test
    fun `launchSingleTop replaces top of same route class`() {
        navigateSettled(DashboardRoute)
        navigateSettled(TreeCaptureRoute("old"))

        navigateSettled(TreeCaptureRoute("new")) { launchSingleTop = true }

        assertEquals(listOf(SplashRoute(), DashboardRoute, TreeCaptureRoute("new")), navigator.backStack)
    }

    @Test
    fun `launchSingleTop pushes when top differs`() {
        navigateSettled(DashboardRoute)

        navigateSettled(SettingsRoute) { launchSingleTop = true }

        assertEquals(listOf(SplashRoute(), DashboardRoute, SettingsRoute), navigator.backStack)
    }

    @Test
    fun `popBackStack pops the top entry`() {
        navigateSettled(DashboardRoute)

        assertTrue(navigator.popBackStack())

        assertEquals(listOf<Any>(SplashRoute()), navigator.backStack)
    }

    @Test
    fun `popBackStack refuses to pop the last entry`() {
        assertFalse(navigator.popBackStack())

        assertEquals(listOf<Any>(SplashRoute()), navigator.backStack)
    }

    @Test
    fun `popBackStackTo non-inclusive pops above target`() {
        navigateSettled(DashboardRoute)
        navigateSettled(TreeCaptureRoute("pic"))
        navigateSettled(TreeImageReviewRoute("photo"))

        assertTrue(navigator.popBackStackTo<TreeCaptureRoute>())

        assertEquals(listOf(SplashRoute(), DashboardRoute, TreeCaptureRoute("pic")), navigator.backStack)
    }

    @Test
    fun `popBackStackTo inclusive pops target as well`() {
        navigateSettled(DashboardRoute)
        navigateSettled(SettingsRoute)

        assertTrue(navigator.popBackStackTo<DashboardRoute>(inclusive = true))

        assertEquals(listOf<Any>(SplashRoute()), navigator.backStack)
    }

    @Test
    fun `popBackStackTo with absent target leaves stack untouched`() {
        navigateSettled(DashboardRoute)

        assertFalse(navigator.popBackStackTo<MapRoute>())

        assertEquals(listOf(SplashRoute(), DashboardRoute), navigator.backStack)
    }

    @Test
    fun `popBackStackTo when target is already top is a no-op`() {
        navigateSettled(DashboardRoute)

        assertFalse(navigator.popBackStackTo<DashboardRoute>())

        assertEquals(listOf(SplashRoute(), DashboardRoute), navigator.backStack)
    }

    @Test
    fun `throttledNavigate drops calls inside the throttle window`() {
        navigator.throttledNavigate(DashboardRoute)
        clock.advance(100)
        navigator.throttledNavigate(SettingsRoute)

        assertEquals(listOf(SplashRoute(), DashboardRoute), navigator.backStack)
    }

    @Test
    fun `throttledNavigate allows calls after the throttle window`() {
        navigator.throttledNavigate(DashboardRoute)
        clock.advance(300)
        navigator.throttledNavigate(SettingsRoute)

        assertEquals(listOf(SplashRoute(), DashboardRoute, SettingsRoute), navigator.backStack)
    }

    @Test
    fun `throttledPopBackStack is debounced after a navigation`() {
        navigator.throttledNavigate(DashboardRoute)
        clock.advance(100)

        assertFalse(navigator.throttledPopBackStack())
        clock.advance(200)
        assertTrue(navigator.throttledPopBackStack())

        assertEquals(listOf<Any>(SplashRoute()), navigator.backStack)
    }

    @Test
    fun `topKey reflects the top of the stack`() {
        assertEquals(SplashRoute(), navigator.topKey)
        navigateSettled(DashboardRoute)
        assertEquals(DashboardRoute, navigator.topKey)
    }

    // --- Origin-scoped throttling (per-screen, no wall-clock gate) ---

    @Test
    fun `scoped navigator navigates while its origin is on top`() {
        val nav = fakeNavigator(DashboardRoute)
        val dashboard = nav.scopedTo(nav.topContentKey)

        dashboard.throttledNavigate(SettingsRoute)

        assertEquals(listOf(DashboardRoute, SettingsRoute), nav.backStack)
    }

    @Test
    fun `scoped navigator drops a second navigation from the same screen (double-tap)`() {
        val nav = fakeNavigator(DashboardRoute)
        val dashboard = nav.scopedTo(nav.topContentKey)

        // Both taps come from Dashboard's button. The first navigates away; by the second
        // tap Dashboard is no longer on top, so it is dropped — no duplicate destination.
        dashboard.throttledNavigate(SettingsRoute)
        dashboard.throttledNavigate(MapRoute)

        assertEquals(listOf(DashboardRoute, SettingsRoute), nav.backStack)
    }

    @Test
    fun `scoped navigators from different screens all navigate with no time gap`() {
        val nav = fakeNavigator(DashboardRoute)

        // Rapidly stepping through a flow: each screen fires its own scoped navigator once.
        // No clock is advanced anywhere — cross-screen navigation is never time-throttled.
        nav.scopedTo(nav.topContentKey).throttledNavigate(UserSelectRoute)
        nav.scopedTo(nav.topContentKey).throttledNavigate(WalletSelectRoute)
        nav.scopedTo(nav.topContentKey).throttledNavigate(AddWalletRoute)

        assertEquals(
            listOf(DashboardRoute, UserSelectRoute, WalletSelectRoute, AddWalletRoute),
            nav.backStack,
        )
    }

    @Test
    fun `scoped throttledPopBackStack drops a second back from the same screen`() {
        val nav = fakeNavigator(DashboardRoute, SettingsRoute)
        val settings = nav.scopedTo(nav.topContentKey)

        assertTrue(settings.throttledPopBackStack())
        assertFalse(settings.throttledPopBackStack())

        assertEquals(listOf<Any>(DashboardRoute), nav.backStack)
    }

    @Test
    fun `scoped navigator with an arg route matches on the full contentKey`() {
        val capture = TreeCaptureRoute("pic")
        val nav = fakeNavigator(DashboardRoute, capture)
        val scoped = nav.scopedTo(nav.topContentKey)

        scoped.throttledNavigate(TreeHeightScreenRoute)

        assertEquals(listOf(DashboardRoute, capture, TreeHeightScreenRoute), nav.backStack)
    }

    // --- Entry identity (contentKey per push) ---

    @Test
    fun `re-pushing a route popped in the same navigation gives it a new contentKey`() {
        val nav = fakeNavigator(DashboardRoute)
        nav.navigate(UserSelectRoute)
        val first = nav.topContentKey
        nav.navigate(AddOrgRoute)

        nav.navigate(UserSelectRoute) { popUpTo<DashboardRoute> { inclusive = true } }

        assertEquals(listOf<Any>(UserSelectRoute), nav.backStack)
        assertNotEquals(first, nav.topContentKey)
    }

    @Test
    fun `popping back reveals the entry with its original contentKey`() {
        val nav = fakeNavigator(DashboardRoute)
        nav.navigate(UserSelectRoute)
        val userSelect = nav.topContentKey
        nav.navigate(AddOrgRoute)

        nav.popBackStack()

        assertEquals(userSelect, nav.topContentKey)
    }

    @Test
    fun `launchSingleTop onto the same route keeps its contentKey`() {
        val nav = fakeNavigator(DashboardRoute)
        nav.navigate(UserSelectRoute)
        val userSelect = nav.topContentKey

        nav.navigate(UserSelectRoute) { launchSingleTop = true }

        assertEquals(userSelect, nav.topContentKey)
    }

    @Test
    fun `a navigation that leaves the stack unchanged keeps the contentKey`() {
        // NavDisplay doesn't rebuild entries for an unchanged stack, so the key must not move.
        val nav = fakeNavigator(DashboardRoute)
        val dashboard = nav.topContentKey

        nav.navigate(DashboardRoute) { popUpTo<DashboardRoute> { inclusive = true } }

        assertEquals(dashboard, nav.topContentKey)
    }

    @Test
    fun `a navigation that leaves the app's back stack unchanged keeps the contentKey`() {
        // The app's back stacks (NavBackStack, SnapshotStateList) compare by identity, not by contents
        val stacks: List<MutableList<NavKey>> = listOf(NavBackStack(DashboardRoute), mutableStateListOf(DashboardRoute))
        for (backStack in stacks) {
            val nav = Navigator(backStack, clock = { 0L })
            val dashboard = nav.topContentKey

            nav.navigate(DashboardRoute) { popUpTo<DashboardRoute> { inclusive = true } }

            assertEquals(backStack::class.simpleName, dashboard, nav.topContentKey)
        }
    }

    @Test
    fun `initial entries use the plain route string as contentKey`() {
        val nav = fakeNavigator(DashboardRoute)

        assertEquals(DashboardRoute.toString(), nav.topContentKey)
    }

    @Test
    fun `ids are dropped once their route leaves the back stack`() {
        val ids = NavEntryIds()
        val nav = Navigator(mutableListOf(DashboardRoute), clock = { 0L }, entryIds = ids)
        nav.navigate(ProfileRoute(planterInfoId = 1))
        nav.popBackStack()

        // Not on the stack any more, so no id is kept for it
        assertEquals(ProfileRoute(planterInfoId = 1).toString(), ids.contentKeyOf(ProfileRoute(planterInfoId = 1)))
    }

    @Test
    fun `entry ids survive a save and restore`() {
        val ids = NavEntryIds()
        val backStack = mutableListOf<NavKey>(DashboardRoute)
        Navigator(backStack, clock = { 0L }, entryIds = ids).navigate(UserSelectRoute)
        val saved = with(NavEntryIds.Saver) { SaverScope { true }.save(ids) }!!

        val restored = NavEntryIds.Saver.restore(saved)!!
        val restoredNav = Navigator(backStack, clock = { 0L }, entryIds = restored)
        restoredNav.navigate(AddOrgRoute)

        assertEquals(ids.contentKeyOf(UserSelectRoute), restored.contentKeyOf(UserSelectRoute))
        // New ids continue after the restored ones instead of reusing them
        assertNotEquals(restored.contentKeyOf(UserSelectRoute).substringAfter('#'), restored.contentKeyOf(AddOrgRoute).substringAfter('#'))
    }
}