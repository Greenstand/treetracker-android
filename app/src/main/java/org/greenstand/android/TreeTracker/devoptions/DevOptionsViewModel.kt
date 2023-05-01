package org.greenstand.android.TreeTracker.devoptions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.utils.updateState

data class DevOptionsState(
    val params: List<Config> = emptyList(),
)

class DevOptionsViewModel(
    private val configurator: Configurator,
    private val convergenceConfiguration: ConvergenceConfiguration
) : ViewModel() {

    private val _state = MutableStateFlow(DevOptionsState())
    val state: Flow<DevOptionsState> = _state

    init {
        val updatedParams = ConfigKeys.configList.map { param ->
            when(param) {
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
        _state.updateState {
            copy(params = updatedParams)
        }
    }

    fun updateParam(param: Config, newValue: Any) {
        configurator.putValue(param, newValue)
        _state.updateState {
            val updatedParamList = params.updateListItem(param) {
                when(this) {
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