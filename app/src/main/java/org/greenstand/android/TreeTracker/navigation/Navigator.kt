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

class NavOptions {
    var launchSingleTop: Boolean = false

    @PublishedApi internal var popUpToMatcher: ((NavKey) -> Boolean)? = null

    @PublishedApi internal var popUpToInclusive: Boolean = false

    internal var popUpToRootRequested: Boolean = false

    class PopUpToBuilder {
        var inclusive: Boolean = false
    }

    inline fun <reified T : NavKey> popUpTo(builder: PopUpToBuilder.() -> Unit = {}) {
        popUpToMatcher = { it is T }
        popUpToInclusive = PopUpToBuilder().apply(builder).inclusive
    }

    fun popUpToRoot() {
        popUpToRootRequested = true
    }
}

class Navigator private constructor(
    val backStack: MutableList<NavKey>,
    private val clock: () -> Long,
    private val origin: Any?,
    private val lastNavigation: LastNavigation,
) {
    constructor(
        backStack: MutableList<NavKey>,
        clock: () -> Long = SystemClock::elapsedRealtime,
        origin: Any? = null,
    ) : this(backStack, clock, origin, LastNavigation())

    private class LastNavigation {
        var at: Long? = null
    }

    private var lastMutationTime = clock() - NAVIGATION_THROTTLE_MS

    val topKey: NavKey? get() = backStack.lastOrNull()

    fun scopedTo(originContentKey: Any?): Navigator = Navigator(backStack, clock, originContentKey, lastNavigation)

    val millisSinceLastNavigation: Long
        get() = lastNavigation.at?.let { clock() - it } ?: Long.MAX_VALUE

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

    fun popBackStack(): Boolean {
        if (backStack.size <= 1) return false
        mutate { backStack.removeAt(backStack.lastIndex) }
        return true
    }

    inline fun <reified T : NavKey> popBackStackTo(inclusive: Boolean = false): Boolean = popBackStackTo({ it is T }, inclusive)

    fun popBackStackTo(
        matcher: (NavKey) -> Boolean,
        inclusive: Boolean,
    ): Boolean {
        val index = backStack.indexOfLast(matcher)
        if (index < 0) return false
        val keepCount = if (inclusive) maxOf(index, 1) else index + 1
        if (keepCount >= backStack.size) return false
        mutate { trimTo(keepCount) }
        return true
    }

    fun throttledNavigate(
        route: NavKey,
        builder: NavOptions.() -> Unit = {},
    ) {
        if (isThrottled()) return
        navigate(route, builder)
    }

    fun throttledPopBackStack(): Boolean {
        if (isThrottled()) return false
        return popBackStack()
    }

    val canNavigate: Boolean get() = !isThrottled()

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

    private fun mutate(block: () -> Unit) {
        Snapshot.withMutableSnapshot(block)
        lastMutationTime = clock()
        lastNavigation.at = lastMutationTime
    }

    private companion object {
        const val NAVIGATION_THROTTLE_MS = 300L
    }
}