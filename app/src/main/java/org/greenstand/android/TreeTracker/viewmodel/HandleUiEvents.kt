/*
 * Copyright 2023 Treetracker
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

/**
 * Subscribes to one-shot [UiEvent]s from a [BaseViewModel] and handles them.
 *
 * Uses `LaunchedEffect(Unit)` so the collection coroutine lives for the composable's
 * entire lifetime and is never cancelled by recomposition. Each [Channel] event is
 * consumed exactly once.
 *
 * Usage: place `HandleUiEvents(viewModel)` near the top of any screen composable.
 */
@Composable
fun <S, A : Action> HandleUiEvents(viewModel: BaseViewModel<S, A>) {
    val navController = LocalNavHostController.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NavigationEvent -> event.navigate(navController)
            }
        }
    }
}