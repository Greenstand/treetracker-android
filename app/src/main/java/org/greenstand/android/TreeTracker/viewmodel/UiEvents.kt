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
import java.util.concurrent.atomic.AtomicBoolean
import org.greenstand.android.TreeTracker.navigation.Navigator

interface UiEvent

class NavigationEvent(
    val navigate: suspend Navigator.() -> Unit,
) : UiEvent

data object PopBackStackEvent : UiEvent

data class ShowSnackbar(
    val message: TextRef,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: TextRef? = null,
    val onAction: (() -> Unit)? = null,
) : UiEvent

class ConsumableEvent<out T : UiEvent>(
    private val content: T,
) {
    private val consumed = AtomicBoolean(false)

    fun getContentIfNotConsumed(): T? = if (consumed.compareAndSet(false, true)) content else null
}