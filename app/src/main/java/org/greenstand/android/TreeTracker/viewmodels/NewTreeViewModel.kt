package org.greenstand.android.TreeTracker.viewmodels

import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.usecases.CreateTreeParams
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.view.CustomToast
import kotlin.math.roundToInt

class NewTreeViewModel(private val sharedPreferences: SharedPreferences,
                       private val userLocationManager: UserLocationManager,
                       private val createTreeUseCase: CreateTreeUseCase,
                       private val analytics: Analytics) : ViewModel() {

    val onTreeSaved: MutableLiveData<Unit> = MutableLiveData()
    val onInsufficientGps: MutableLiveData<Unit> = MutableLiveData()
    val navigateToTreeHeight: MutableLiveData<NewTree> = MutableLiveData()
    val navigateBack: MutableLiveData<Unit> = MutableLiveData()
    val onTakePicture: MutableLiveData<Unit> = MutableLiveData()

    val noteEnabledLiveData: LiveData<Boolean> = MutableLiveData<Boolean>().apply {
        postValue(FeatureFlags.TREE_NOTE_FEATURE_ENABLED)
    }

    val accuracyLiveData: LiveData<Int> = MutableLiveData<Int>().apply {
        postValue(userLocationManager.currentLocation?.accuracy?.roundToInt() ?: 0)
    }

    var photoPath: String? = null

    init {
        if (userLocationManager.currentLocation == null) {
            onInsufficientGps.postValue(Unit)
            navigateBack.postValue(Unit)
        } else if (photoPath == null) {
            onTakePicture.postValue(Unit)
        }
    }

    suspend fun createTree(note: String) {

        val newTree = createNewTree(note, photoPath!!)

        if (newTree.content.isNotBlank()) {
            analytics.treeNoteAdded(newTree.content.length)
        }

        if (FeatureFlags.TREE_HEIGHT_FEATURE_ENABLED) {
            navigateToTreeHeight.postValue(newTree)
        } else {
            saveTree(newTree)
            onTreeSaved.postValue(Unit)
            navigateBack.postValue(Unit)
        }
    }

    private suspend fun saveTree(newTree: NewTree): Long {
        val createTreeParams = CreateTreeParams(
            planterCheckInId = newTree.planterCheckInId,
            photoPath = newTree.photoPath,
            content = newTree.content
        )

        return createTreeUseCase.execute(createTreeParams)
    }

    fun isImageBlurry(data: Intent): Boolean {
        val imageQuality = data.getDoubleExtra(ValueHelper.FOCUS_METRIC_VALUE, 0.0)
        return imageQuality < FOCUS_THRESHOLD
    }

    private fun createNewTree(note: String, photoPath: String): NewTree {
        val planterCheckinId = sharedPreferences.getLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)

        return NewTree(
            photoPath,
            note,
            planterCheckinId
        )
    }

    companion object {
        const val FOCUS_THRESHOLD = 700.0
    }

}