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

import android.os.SystemClock
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavKey

val LocalNavigator = compositionLocalOf<Navigator> { error("No Navigator found!") }

/**
 * Navigation options mirroring the Navigation 2 `NavOptionsBuilder` surface so
 * call sites keep the same DSL shape: `navigate(route) { popUpTo<X> { inclusive = true };
 * launchSingleTop = true }`.
 */
class NavOptions {
    /**
     * When the top entry is already the same route class, replace it instead of
     * pushing a duplicate. Unlike Nav2 (which kept the existing entry), a match
     * with different arguments recreates the entry with the new arguments.
     */
    var launchSingleTop: Boolean = false

    @PublishedApi internal var popUpToMatcher: ((NavKey) -> Boolean)? = null

    @PublishedApi internal var popUpToInclusive: Boolean = false

    internal var popUpToRootRequested: Boolean = false

    class PopUpToBuilder {
        var inclusive: Boolean = false
    }

    /**
     * Pop all entries above the most recent [T] before navigating; with
     * `inclusive = true`, pop [T] itself as well. If [T] is not on the back
     * stack, nothing is popped (Nav2 parity) and the navigation still happens.
     */
    inline fun <reified T : NavKey> popUpTo(builder: PopUpToBuilder.() -> Unit = {}) {
        popUpToMatcher = { it is T }
        popUpToInclusive = PopUpToBuilder().apply(builder).inclusive
    }

    /** Clear the entire back stack before navigating. Replaces Nav2's `popUpTo(graph.id) { inclusive = true }`. */
    fun popUpToRoot() {
        popUpToRootRequested = true
    }
}

/**
 * App-owned navigation state and operations over a Navigation 3 back stack.
 *
 * Replaces `NavHostController`: the [backStack] is the single source of truth
 * (a `NavBackStack` in production, any `MutableList` in tests) and `NavDisplay`
 * renders whatever it contains.
 *
 * ## Double-tap protection
 *
 * The `throttled*` variants exist to stop a screen firing the same navigation twice
 * (e.g. a rapid double-tap on a button while the screen is still fading out). How they
 * decide to drop a call depends on whether the navigator is bound to an [origin] screen:
 *
 * - **Origin-scoped** (the production path — every screen gets one via
 *   `rememberScreenTrackingNavEntryDecorator`, keyed by its entry's contentKey): a
 *   throttled call is allowed only while [origin] is still the top of the back stack.
 *   The first tap navigates away, so the origin is no longer on top and any further
 *   tap from that same screen is dropped — while a first tap from *each* screen always
 *   passes. This is what lets a user move through many screens as fast as they like yet
 *   never double-navigate from one screen. It reimplements Nav2's "only navigate while
 *   the current entry is RESUMED" guard without a wall-clock timer.
 * - **Unscoped** ([origin] is null — tests, or any host not inside a `NavDisplay`
 *   entry): falls back to a [NAVIGATION_THROTTLE_MS] time-based debounce after every
 *   successful stack mutation.
 *
 * Non-throttled [navigate]/[popBackStack]/[popBackStackTo] are never gated; they are for
 * programmatic, single-shot transitions (splash auto-advance, flow controllers, …).
 *
 * @param origin the contentKey of the entry this navigator is bound to, or null for an
 *   unscoped navigator. Set indirectly via [scopedTo].
 */
class Navigator(
    val backStack: MutableList<NavKey>,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val origin: Any? = null,
) {
    // Start outside the throttle window so the first navigation is never dropped.
    private var lastMutationTime = clock() - NAVIGATION_THROTTLE_MS

    val topKey: NavKey? get() = backStack.lastOrNull()

    /**
     * Returns a view of this navigator bound to [originContentKey] (the contentKey of the
     * entry the caller is composed in), sharing the same [backStack]. Throttled operations
     * on the returned navigator are dropped unless [originContentKey] is still the top of
     * the stack — i.e. you can only navigate *away from* a screen while that screen is on
     * top. See the class doc for the rationale.
     */
    fun scopedTo(originContentKey: Any?): Navigator = Navigator(backStack, clock, originContentKey)

    /** Push [route], honoring [NavOptions] (popUpTo / popUpToRoot / launchSingleTop). */
    fun navigate(
        route: NavKey,
        builder: NavOptions.() -> Unit = {},
    ) {
        val options = NavOptions().apply(builder)
        mutate {
            val matcher = options.popUpToMatcher
            when {
                options.popUpToRootRequested -> backStack.clear()
                matcher != null -> {
                    val index = backStack.indexOfLast(matcher)
                    if (index >= 0) {
                        trimTo(if (options.popUpToInclusive) index else index + 1)
                    }
                }
            }
            val top = backStack.lastOrNull()
            if (options.launchSingleTop && top != null && top::class == route::class) {
                backStack[backStack.lastIndex] = route
            } else {
                backStack.add(route)
            }
        }
    }

    /** Pop the top entry. Never pops the last remaining entry (Nav2 parity). */
    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        mutate { backStack.removeAt(backStack.lastIndex) }
        return true
    }

    /**
     * Pop entries until the most recent [T] is on top (or removed, when [inclusive]).
     * Returns false and leaves the stack untouched if [T] is absent or nothing would pop.
     * Replaces Nav2's `popBackStack<T>(inclusive)`.
     */
    inline fun <reified T : NavKey> popBackStackTo(inclusive: Boolean = false): Boolean = popBackStackTo({ it is T }, inclusive)

    fun popBackStackTo(
        matcher: (NavKey) -> Boolean,
        inclusive: Boolean,
    ): Boolean {
        val index = backStack.indexOfLast(matcher)
        if (index < 0) return false
        // Never pop the last remaining entry, even when inclusive.
        val keepCount = if (inclusive) maxOf(index, 1) else index + 1
        if (keepCount >= backStack.size) return false
        mutate { trimTo(keepCount) }
        return true
    }

    /** [navigate], dropped silently when this screen may not navigate (see class doc). */
    fun throttledNavigate(
        route: NavKey,
        builder: NavOptions.() -> Unit = {},
    ) {
        if (isThrottled()) return
        navigate(route, builder)
    }

    /** [popBackStack], dropped silently when this screen may not navigate (see class doc). */
    fun throttledPopBackStack(): Boolean {
        if (isThrottled()) return false
        return popBackStack()
    }

    /**
     * Whether a throttled call should be dropped. Origin-scoped navigators gate on
     * "is my screen still on top?"; unscoped navigators fall back to a time debounce.
     */
    private fun isThrottled(): Boolean =
        if (origin != null) {
            origin != topKey?.toString()
        } else {
            clock() - lastMutationTime < NAVIGATION_THROTTLE_MS
        }

    private fun trimTo(keepCount: Int) {
        while (backStack.size > keepCount) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /** Applies multi-step stack changes atomically so they land in a single frame. */
    private fun mutate(block: () -> Unit) {
        Snapshot.withMutableSnapshot(block)
        lastMutationTime = clock()
    }

    private companion object {
        const val NAVIGATION_THROTTLE_MS = 300L
    }
}