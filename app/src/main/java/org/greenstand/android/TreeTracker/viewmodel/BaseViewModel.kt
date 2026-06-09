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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base ViewModel that exposes:
 *
 * - `state: StateFlow<S>` — the screen's persistent UI state, survives recomposition.
 * - `events: SharedFlow<ConsumableEvent<UiEvent>>` — one-shot UI signals
 *   (navigation, snackbars, etc.) delivered to the screen via [HandleUIEvents].
 *
 * The event flow uses `replay = 5` so events emitted before the first [HandleUIEvents]
 * collector subscribes (the common case for events fired from `init`) are still
 * delivered. Each event is wrapped in [ConsumableEvent] so the replay doesn't
 * cause the same event to fire twice.
 */
abstract class BaseViewModel<S, A : Action>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events =
        MutableSharedFlow<ConsumableEvent<UiEvent>>(
            replay = EVENT_REPLAY,
            extraBufferCapacity = EVENT_EXTRA_BUFFER,
        )
    val events: SharedFlow<ConsumableEvent<UiEvent>> = _events.asSharedFlow()

    protected val currentState: S get() = _state.value

    protected fun updateState(update: S.() -> S) {
        _state.value = _state.value.update()
    }

    protected fun sendEvent(event: UiEvent) {
        _events.tryEmit(ConsumableEvent(event))
    }

    /** Backwards-compatible alias for [sendEvent]. */
    protected fun triggerEvent(event: UiEvent) = sendEvent(event)

    /** Convenience for the common case `sendEvent(NavigationEvent { navigate(route) })`. */
    protected fun navigate(route: Any) {
        sendEvent(NavigationEvent { navigate(route) })
    }

    abstract fun handleAction(action: A)

    private companion object {
        // Replay so events emitted before the first HandleUIEvents subscriber (typically
        // from init) are not dropped. ConsumableEvent makes the replay idempotent.
        const val EVENT_REPLAY = 5
        const val EVENT_EXTRA_BUFFER = 16
    }
}
