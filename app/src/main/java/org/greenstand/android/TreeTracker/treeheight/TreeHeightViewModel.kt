package org.greenstand.android.TreeTracker.treeheight

import androidx.compose.material.ButtonColors
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.view.AppButtonColors


data class TreeHeightSelectionState(
    val colours: List<ButtonColors> = listOf(AppButtonColors.ProgressGreen,AppButtonColors.MessagePurple,AppButtonColors.Yellow,AppButtonColors.SkyBlue,AppButtonColors.UploadOrange),
    val selectedColour: ButtonColors? = null,
)

class TreeHeightSelectionViewModel(
) : ViewModel() {
    private val _state = MutableLiveData<TreeHeightSelectionState>()
    val state: LiveData<TreeHeightSelectionState> = _state

    fun selectColor(colors: ButtonColors) {
        _state.value = _state.value?.copy(
            selectedColour = colors
        )
    }
}