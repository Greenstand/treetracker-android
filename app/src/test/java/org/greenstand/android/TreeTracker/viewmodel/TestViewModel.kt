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

import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.navigation.NavOptions

data class TestState(
    val count: Int = 0,
    val label: String = "",
)

sealed interface TestAction : Action {
    data object Increment : TestAction

    data class Rename(
        val label: String,
    ) : TestAction
}

/** A feature-specific event, for exercising custom [UiEvent] handling. */
data class TestEvent(
    val id: Int,
) : UiEvent

/** Minimal [BaseViewModel] that exposes its protected helpers to tests. */
class TestViewModel : BaseViewModel<TestState, TestAction>(TestState()) {
    override fun handleAction(action: TestAction) {
        when (action) {
            TestAction.Increment -> updateState { copy(count = count + 1) }
            is TestAction.Rename -> updateState { copy(label = action.label) }
        }
    }

    fun readCurrentState(): TestState = currentState

    fun emit(event: UiEvent) = sendEvent(event)

    fun emitViaTrigger(event: UiEvent) = triggerEvent(event)

    fun navigateTo(route: NavKey) = navigate(route)

    fun navigateWithOptions(
        route: NavKey,
        builder: NavOptions.() -> Unit,
    ) = navigate(route, builder)

    fun goBack() = popBackStack()
}