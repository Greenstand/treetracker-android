package org.greenstand.android.TreeTracker.devoptions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.updateState

data class DevOptionsState(
    val params: List<Config> = DevConfig.configList,
)

class DevOptionsViewModel(
    private val prefs: Preferences,
) : ViewModel() {

    private val _state = MutableStateFlow(DevOptionsState())
    val state: Flow<DevOptionsState> = _state

    init {
        val updatedParams = _state.value.params.map { param ->
            param.copy(
                defaultValue = prefs.getBoolean(param.key, param.defaultValue)
            )
        }
        _state.updateState {
            copy(params = updatedParams)
        }
    }

    fun updateParam(param: Config, newValue: Boolean) {
        prefs.edit().putBoolean(param.key, newValue).commit()
        _state.updateState {
            val updatedParamList = params.updateListItem(param) {
                copy(defaultValue = newValue)
            }
            copy(params = updatedParamList)
        }
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