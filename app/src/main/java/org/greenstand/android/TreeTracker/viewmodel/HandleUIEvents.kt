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
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import timber.log.Timber

/**
 * Subscribes to one-shot [UiEvent]s from a [BaseViewModel] and dispatches them.
 *
 * Built-in handling:
 * - [NavigationEvent] → invokes the lambda with the current [NavHostController].
 * - [NavigateUpEvent] → `navController.navigateUp()`.
 * - [ShowSnackbar]    → forwarded to the app-wide [SnackbarController].
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
    val navController = LocalNavHostController.current
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { consumable ->
            val event = consumable.getContentIfNotConsumed() ?: return@collect

            val handled = onEvent?.invoke(event) ?: false
            if (handled) return@collect

            when (event) {
                is NavigationEvent -> event.navigate(navController)
                is NavigateUpEvent -> navController.navigateUp()
                is ShowSnackbar -> snackbarController.show(event)
                else -> Timber.w("Unhandled UiEvent: $event")
            }
        }
    }
}
