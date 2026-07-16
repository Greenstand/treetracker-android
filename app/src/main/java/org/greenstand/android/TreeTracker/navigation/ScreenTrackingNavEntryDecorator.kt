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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.koin.compose.koinInject

private const val TRANSITION_DURATION_MS = 300

val FastFadeIn: EnterTransition = fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
val FastFadeOut: ExitTransition = fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))

/**
 * The [androidx.navigation3.runtime.NavEntry.contentKey] of the entry hosting the current
 * composition, or null outside of a `NavDisplay` entry. `HandleUIEvents` compares it against
 * `Navigator.topKey` to drop navigation events replayed from screens that are no longer on top.
 */
val LocalNavEntryContentKey = compositionLocalOf<Any?> { null }

/**
 * Decorates every entry with Crashlytics screen tracking (replacing Nav2's
 * `trackedComposable` wrapper), provides [LocalNavEntryContentKey], and re-provides
 * [LocalNavigator] as an [Navigator.scopedTo] view bound to this entry.
 *
 * The scoped navigator is what gives each screen its own double-tap protection: a
 * throttled navigation only fires while this entry is the top of the stack, so rapid
 * taps across different screens all succeed while a repeated tap on one screen is
 * dropped. See [Navigator] for details.
 *
 * The screen name is derived from the entry's contentKey, which defaults to the
 * route's `toString()`. All routes are `data object`s or `data class`es, so this
 * yields `RouteName` or `RouteName(arg=...)` — matching the `T::class.simpleName`
 * value reported before the Navigation 3 migration.
 */
@Composable
fun rememberScreenTrackingNavEntryDecorator(): NavEntryDecorator<NavKey> {
    val exceptionDataCollector = koinInject<ExceptionDataCollector>()
    return remember(exceptionDataCollector) {
        NavEntryDecorator<NavKey> { entry ->
            val screenName = entry.contentKey.toString().substringBefore('(')
            LaunchedEffect(screenName) {
                exceptionDataCollector.setScreen(screenName)
            }
            val baseNavigator = LocalNavigator.current
            val scopedNavigator =
                remember(baseNavigator, entry.contentKey) {
                    baseNavigator.scopedTo(entry.contentKey)
                }
            CompositionLocalProvider(
                LocalNavEntryContentKey provides entry.contentKey,
                LocalNavigator provides scopedNavigator,
            ) {
                entry.Content()
            }
        }
    }
}