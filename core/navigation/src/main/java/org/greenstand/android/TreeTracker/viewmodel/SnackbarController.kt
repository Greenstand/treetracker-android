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

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide bus for [ShowSnackbar] events. A single instance is provided at the root via
 * [LocalSnackbarController] and a single `SnackbarHost` consumes it, so any ViewModel can
 * raise a snackbar with `sendEvent(ShowSnackbar(TextRef.Res(R.string.something)))`
 * without each screen wiring its own host.
 */
class SnackbarController {
    private val _events = MutableSharedFlow<ShowSnackbar>(extraBufferCapacity = 16)
    val events: SharedFlow<ShowSnackbar> = _events.asSharedFlow()

    fun show(event: ShowSnackbar) {
        _events.tryEmit(event)
    }
}

/**
 * CompositionLocal that gives any Composable access to the app's [SnackbarController].
 * [HandleUIEvents] uses it to forward `ShowSnackbar` events; [org.greenstand.android.TreeTracker.root.Root]
 * provides it.
 */
val LocalSnackbarController =
    compositionLocalOf<SnackbarController> {
        error("SnackbarController not provided. Wrap your content in Root.")
    }