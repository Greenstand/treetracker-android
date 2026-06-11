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
import androidx.navigation.NavHostController
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base interface for one-shot UI events emitted by ViewModels.
 *
 * Unlike `state`, events are consumed exactly once. The replay buffer on
 * [BaseViewModel.events] ensures events emitted before the first collector subscribes
 * (e.g. from `init`) are still delivered; [ConsumableEvent] makes sure the replay
 * doesn't fire the event twice.
 *
 * Built-in subtypes ([NavigationEvent], [PopBackStackEvent], [ShowSnackbar]) are handled
 * automatically by [HandleUIEvents]. Feature-specific events implement this interface
 * and are intercepted via the optional `onEvent` parameter.
 */
interface UiEvent

/**
 * A navigation event carrying a suspend lambda that receives the [NavHostController].
 * Allows any navigation operation (navigate, popBackStack, deep navigation flows) and
 * supports suspend calls like `CaptureFlowNavigationController.navForward`.
 *
 * Not a `data class` because lambda fields produce meaningless equals/hashCode.
 */
class NavigationEvent(
    val navigate: suspend NavHostController.() -> Unit,
) : UiEvent

/** Pop the back stack one step. Dispatched via `throttledPopBackStack` for debouncing. */
data object PopBackStackEvent : UiEvent

/**
 * Show a snackbar through the global [SnackbarController]. The [message] is held as a
 * [TextRef] so the ViewModel does not need a [android.content.Context].
 */
data class ShowSnackbar(
    val message: TextRef,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: TextRef? = null,
    val onAction: (() -> Unit)? = null,
) : UiEvent

/**
 * Thread-safe wrapper ensuring a [UiEvent] is consumed at most once.
 *
 * Because [BaseViewModel.events] uses a `replay` buffer, a single emitted event can be
 * delivered to multiple collectors (e.g. after rotation) or replayed to a new collector
 * after the screen is recreated. Wrapping the event in [ConsumableEvent] guarantees that
 * only the first call to [getContentIfNotConsumed] returns the event.
 */
class ConsumableEvent<out T : UiEvent>(
    private val content: T,
) {
    private val consumed = AtomicBoolean(false)

    /** Returns the event on the first call, `null` on every subsequent call. */
    fun getContentIfNotConsumed(): T? = if (consumed.compareAndSet(false, true)) content else null
}