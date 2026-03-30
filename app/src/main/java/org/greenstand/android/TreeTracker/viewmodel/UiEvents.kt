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