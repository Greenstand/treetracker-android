package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.data.TreeColor
import org.greenstand.android.TreeTracker.data.TreeHeightAttributes
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase

class TreeHeightViewModel(
    private val createTreeUseCase: CreateTreeUseCase,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics,
    private val stepCounter: StepCounter
) : ViewModel() {

    var newTree: Tree? = null
    var treeColor: TreeColor? = null
        set(value) {
            field = value
            onEnableButtonLiveData.postValue(value != null)
        }

    private val toastMessageLiveData = MutableLiveData<Int>()
    private val onFinishedLiveData = MutableLiveData<Unit>()
    private val onEnableButtonLiveData = MutableLiveData<Boolean>()

    fun saveNewTree() {
        viewModelScope.launch {
            if (treeColor == null) {
                toastMessageLiveData.postValue(R.string.tree_height_selection_error)
                return@launch
            }

            newTree
                ?.let { tree ->
                    withContext(Dispatchers.IO) {
                        with(TreeHeightAttributes(heightColor = treeColor!!)) {
                            tree.addTreeAttribute(
                                Tree.Attributes.TREE_COLOR_ATTR_KEY, heightColor.value)
                            tree.addTreeAttribute(
                                Tree.Attributes.APP_BUILD_ATTR_KEY, appBuild)
                            tree.addTreeAttribute(
                                Tree.Attributes.APP_FLAVOR_ATTR_KEY, appFlavor)
                            tree.addTreeAttribute(
                                Tree.Attributes.APP_VERSION_ATTR_KEY, appVersion)
                        }
                        createTreeUseCase.execute(tree)
                    }
                }
                ?.also {
                    toastMessageLiveData.postValue(R.string.tree_saved)
                    analytics.treeHeightMeasured(treeColor!!)
                    onFinishedLiveData.postValue(Unit)
                    // Assign the current absolute step count to 'absoluteStepCountOnTreeCapture'
                    // variable to calculate future step count deltas
                    stepCounter.absoluteStepCountOnTreeCapture = stepCounter.absoluteStepCount
                    stepCounter.disable()
                }
                ?: run { toastMessageLiveData.postValue(R.string.tree_height_save_error) }
        }
    }

    fun toastMessagesLiveData(): LiveData<Int> = toastMessageLiveData

    fun onFinishedLiveData(): LiveData<Unit> = onFinishedLiveData

    fun onEnableButtonLiveData(): LiveData<Boolean> = onEnableButtonLiveData
}
