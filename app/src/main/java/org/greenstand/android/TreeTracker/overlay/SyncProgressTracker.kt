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
package org.greenstand.android.TreeTracker.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncStep(
    val displayName: String,
) {
    MESSAGES("Messages"),
    DEVICE_CONFIG("Device Config"),
    USERS("Users"),
    SESSIONS("Sessions"),
    LEGACY_TREES("Legacy Trees"),
    TREES("Trees"),
    LOCATIONS("Locations"),
}

enum class StepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    ERROR,
}

data class SyncStepState(
    val step: SyncStep,
    val status: StepStatus = StepStatus.PENDING,
    val itemsTotal: Int = 0,
    val itemsCompleted: Int = 0,
    val errorMessage: String? = null,
)

data class SyncProgressState(
    val isActive: Boolean = false,
    val currentStep: SyncStep? = null,
    val steps: List<SyncStepState> = SyncStep.values().map { SyncStepState(it) },
    val overallError: String? = null,
)

class SyncProgressTracker {
    private val _state = MutableStateFlow(SyncProgressState())
    val state: StateFlow<SyncProgressState> = _state.asStateFlow()

    fun startSync() {
        _state.value =
            SyncProgressState(
                isActive = true,
                steps = SyncStep.values().map { SyncStepState(it) },
            )
    }

    fun startStep(step: SyncStep) {
        _state.value =
            _state.value.copy(
                currentStep = step,
                steps =
                    _state.value.steps.map {
                        if (it.step == step) it.copy(status = StepStatus.RUNNING) else it
                    },
            )
    }

    fun updateStepProgress(
        step: SyncStep,
        completed: Int,
        total: Int,
    ) {
        _state.value =
            _state.value.copy(
                steps =
                    _state.value.steps.map {
                        if (it.step == step) {
                            it.copy(itemsCompleted = completed, itemsTotal = total)
                        } else {
                            it
                        }
                    },
            )
    }

    fun completeStep(step: SyncStep) {
        _state.value =
            _state.value.copy(
                steps =
                    _state.value.steps.map {
                        if (it.step == step) it.copy(status = StepStatus.SUCCESS) else it
                    },
            )
    }

    fun failStep(
        step: SyncStep,
        error: String?,
    ) {
        _state.value =
            _state.value.copy(
                steps =
                    _state.value.steps.map {
                        if (it.step == step) {
                            it.copy(status = StepStatus.ERROR, errorMessage = error)
                        } else {
                            it
                        }
                    },
            )
    }

    fun endSync(error: String? = null) {
        _state.value =
            _state.value.copy(
                isActive = false,
                currentStep = null,
                overallError = error,
            )
    }
}