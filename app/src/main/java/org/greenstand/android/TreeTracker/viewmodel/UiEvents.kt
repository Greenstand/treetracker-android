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

import androidx.navigation.NavHostController
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base interface for one-shot UI events emitted by ViewModels.
 * Unlike state, events are consumed exactly once and not replayed on recomposition.
 */
interface UiEvent

/**
 * A navigation event carrying a suspend lambda that receives [NavHostController] as receiver.
 * This allows any navigation operation (navigate, popBackStack, etc.) including suspend calls
 * like [CaptureFlowNavigationController.navForward].
 *
 * Not a data class because lambda fields produce meaningless equals/hashCode.
 */
class NavigationEvent(
    val navigate: suspend NavHostController.() -> Unit,
) : UiEvent

/**
 * Thread-safe wrapper ensuring a [UiEvent] is consumed at most once.
 *
 * Events buffered in a [Channel] can outlive the collector that was meant to
 * handle them (e.g. rotation, back-navigation to a screen whose ViewModel
 * survived). Wrapping every event in [ConsumableEvent] guarantees that even
 * if a stale event is delivered to a new collector, [getContentIfNotConsumed]
 * returns `null` after the first access.
 */
class ConsumableEvent<out T : UiEvent>(
    private val content: T,
) {
    private val consumed = AtomicBoolean(false)

    /** Returns the event on the first call, `null` on every subsequent call. */
    fun getContentIfNotConsumed(): T? = if (consumed.compareAndSet(false, true)) content else null
}