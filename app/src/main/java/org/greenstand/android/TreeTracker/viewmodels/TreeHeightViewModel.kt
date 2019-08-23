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
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.data.TreeColor
import org.greenstand.android.TreeTracker.data.TreeHeightAttributes
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.usecases.CreateTreeParams
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase

class TreeHeightViewModel(private val createTreeUseCase: CreateTreeUseCase,
                          private val dao: TreeTrackerDAO,
                          private val analytics: Analytics) : ViewModel() {

    var newTree: NewTree? = null
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

                        val createTreeParams = CreateTreeParams(
                            planterCheckInId = tree.planterCheckInId,
                            photoPath = tree.photoPath,
                            content = tree.content
                        )

                        val treeId = createTreeUseCase.execute(createTreeParams)

                        fun addKeyValueAttribute(key: String, value: String) {
                            val entity =
                                TreeAttributeEntity(
                                    key = key,
                                    value = value,
                                    treeCaptureId = treeId
                                )
                            dao.insertTreeAttribute(entity)
                        }

                        with(TreeHeightAttributes(treeId = treeId, heightColor = treeColor!!)) {
                            addKeyValueAttribute(TreeManager.TREE_COLOR_ATTR_KEY, heightColor.value)
                            addKeyValueAttribute(TreeManager.APP_BUILD_ATTR_KEY, appBuild)
                            addKeyValueAttribute(TreeManager.APP_FLAVOR_ATTR_KEY, appFlavor)
                            addKeyValueAttribute(TreeManager.APP_VERSION_ATTR_KEY, appVersion)
                        }
                    }
                }
                ?.also {
                    toastMessageLiveData.postValue(R.string.tree_saved)
                    analytics.treeHeightMeasured(treeColor!!)
                    onFinishedLiveData.postValue(Unit)
                }
                ?: run { toastMessageLiveData.postValue(R.string.tree_height_save_error) }

        }
    }

    fun toastMessagesLiveData(): LiveData<Int> = toastMessageLiveData

    fun onFinishedLiveData(): LiveData<Unit> = onFinishedLiveData

    fun onEnableButtonLiveData(): LiveData<Boolean> = onEnableButtonLiveData
}