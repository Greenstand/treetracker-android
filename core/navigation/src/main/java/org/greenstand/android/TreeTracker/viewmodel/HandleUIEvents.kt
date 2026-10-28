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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.greenstand.android.TreeTracker.navigation.LocalNavEntryContentKey
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.Navigator
import timber.log.Timber

/**
 * Subscribes to one-shot [UiEvent]s from a [BaseViewModel] and dispatches them.
 *
 * Built-in handling:
 * - [NavigationEvent] → invokes the lambda with the current [Navigator].
 * - [PopBackStackEvent] → `navigator.throttledPopBackStack()`.
 * - [ShowSnackbar]    → forwarded to the app-wide [SnackbarController].
 *
 * Navigation events are only executed while the hosting entry is the top of the back
 * stack — the Navigation 3 equivalent of Nav2's "current entry is RESUMED" guard. This
 * drops events replayed from screens the user has already left.
 *
 * Custom handling: pass [onEvent] to intercept any event. Return `true` to mark the event
 * as handled and skip the default handler; return `false` to fall through to the
 * built-in dispatch.
 *
 * Place `HandleUIEvents(viewModel)` near the top of each screen's root Composable.
 */
@Composable
fun <S, A : Action> HandleUIEvents(
    viewModel: BaseViewModel<S, A>,
    onEvent: ((UiEvent) -> Boolean)? = null,
) {
    val navigator = LocalNavigator.current
    val entryContentKey = LocalNavEntryContentKey.current
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { consumable ->
            val event = consumable.getContentIfNotConsumed() ?: return@collect

            val handled = onEvent?.invoke(event) ?: false
            if (handled) return@collect

            when (event) {
                is NavigationEvent ->
                    if (isHostingEntryOnTop(entryContentKey, navigator)) {
                        event.navigate(navigator)
                    }
                is PopBackStackEvent ->
                    if (isHostingEntryOnTop(entryContentKey, navigator)) {
                        navigator.throttledPopBackStack()
                    }
                is ShowSnackbar -> snackbarController.show(event)
                else -> Timber.w("Unhandled UiEvent: $event")
            }
        }
    }
}

/**
 * True when the entry hosting this composition is the current top of the back stack,
 * or when the composition is not hosted inside a nav entry at all (contentKey == null).
 * Entry contentKeys default to the route's `toString()`, so compare against the top
 * key's string form.
 */
private fun isHostingEntryOnTop(
    entryContentKey: Any?,
    navigator: Navigator,
): Boolean = entryContentKey == null || entryContentKey == navigator.topKey?.toString()