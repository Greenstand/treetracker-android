package org.greenstand.android.TreeTracker.capture

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

data class TreeCaptureState(
    val profilePicUrl: String,
)

class TreeCaptureViewModel(profilePicUrl: String) : ViewModel() {

    private val _state = MutableLiveData(TreeCaptureState(profilePicUrl))
    val state: LiveData<TreeCaptureState> = _state

}

class TreeCaptureViewModelFactory(private val profilePicUrl: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        Log.d("JONATHAN", "FACTORY")
        return TreeCaptureViewModel(profilePicUrl) as T
    }
}