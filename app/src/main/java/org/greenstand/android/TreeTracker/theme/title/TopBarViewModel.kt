package org.greenstand.android.TreeTracker.theme.title

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.R


data class TopBarState(
    val icon: Int = R.drawable.greenstand_logo
)

class TopBarViewModel : ViewModel() {
    private val _state = MutableLiveData<TopBarState>()
    val state: LiveData<TopBarState> = _state
    //TODO Create function that updates icon state depending on selected organization and downloaded resources
}