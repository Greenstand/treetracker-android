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

val LocalNavEntryContentKey = compositionLocalOf<Any?> { null }

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