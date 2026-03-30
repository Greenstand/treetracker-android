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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

abstract class BaseViewModel<S, A : Action>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events = Channel<ConsumableEvent<UiEvent>>(Channel.BUFFERED)
    val events: Flow<ConsumableEvent<UiEvent>> = _events.receiveAsFlow()

    protected val currentState: S get() = _state.value

    protected fun updateState(update: S.() -> S) {
        _state.value = _state.value.update()
    }

    protected fun triggerEvent(event: UiEvent) {
        _events.trySend(ConsumableEvent(event))
    }

    protected fun navigate(route: Any) {
        triggerEvent(NavigationEvent { navigate(route) })
    }

    abstract fun handleAction(action: A)
}