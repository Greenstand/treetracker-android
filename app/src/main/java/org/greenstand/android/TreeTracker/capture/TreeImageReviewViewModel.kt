package org.greenstand.android.TreeTracker.capture

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.Users

data class TreeImageReviewState(
    val note: String = "",
)

class TreeImageReviewViewModel(
    private val treeCapturer: TreeCapturer,
    private val users: Users,
) : ViewModel() {

    private val _state = MutableLiveData(TreeImageReviewState())
    val state: LiveData<TreeImageReviewState> = _state

    val profilePicPath: String = users.currentSessionUser?.photoPath ?: ""

    fun approveImage() {
        viewModelScope.launch {
            treeCapturer.saveTree()
        }
    }

}

//class TreeImageReviewViewModelFactory(private val imagePath: String)
//    : ViewModelProvider.Factory, KoinComponent {
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//        return TreeImageReviewViewModel(imagePath, get()) as T
//    }
//}