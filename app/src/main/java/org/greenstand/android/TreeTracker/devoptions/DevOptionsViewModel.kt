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
package org.greenstand.android.TreeTracker.devoptions

import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class DevOptionsState(
    val params: List<Config> = emptyList(),
)

sealed class DevOptionsAction : Action {
    data class UpdateParam(val param: Config, val newValue: Any) : DevOptionsAction()
    object NavigateBack : DevOptionsAction()
}

class DevOptionsViewModel(
    private val configurator: Configurator,
    private val convergenceConfiguration: ConvergenceConfiguration
) : BaseViewModel<DevOptionsState, DevOptionsAction>(DevOptionsState()) {

    init {
        val updatedParams = ConfigKeys.configList.map { param ->
            when (param) {
                is BooleanConfig -> param.copy(
                    defaultValue = configurator.getBoolean(param)
                )
                is IntConfig -> param.copy(
                    defaultValue = configurator.getInt(param)
                )
                is FloatConfig -> param.copy(
                    defaultValue = configurator.getFloat(param)
                )
            }
        }
        updateState { copy(params = updatedParams) }
    }

    override fun handleAction(action: DevOptionsAction) {
        when (action) {
            is DevOptionsAction.UpdateParam -> updateParam(action.param, action.newValue)
            else -> { }
        }
    }

    private fun updateParam(param: Config, newValue: Any) {
        configurator.putValue(param, newValue)
        updateState {
            val updatedParamList = params.updateListItem(param) {
                when (this) {
                    is BooleanConfig -> copy(defaultValue = newValue as Boolean)
                    is IntConfig -> copy(defaultValue = newValue as Int)
                    is FloatConfig -> copy(defaultValue = newValue as Float)
                }
            }
            copy(params = updatedParamList)
        }
    }

    override fun onCleared() {
        convergenceConfiguration.refreshConfig()
        super.onCleared()
    }
}

fun <T> List<T>.updateListItem(item: T, onUpdate: T.() -> T): List<T> {
    return this.toMutableList().updateMutableItem(item, onUpdate)
}

fun <T> MutableList<T>.updateMutableItem(item: T, onUpdate: T.() -> T): List<T> {
    val indexToUpdate = this.indexOf(item)
    this[indexToUpdate]?.onUpdate()?.let { updatedItem ->
        this.removeAt(indexToUpdate)
        this.add(indexToUpdate, updatedItem)
    }
    return this
}
