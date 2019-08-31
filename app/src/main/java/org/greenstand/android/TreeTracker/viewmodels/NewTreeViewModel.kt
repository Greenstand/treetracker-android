package org.greenstand.android.TreeTracker.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.data.NewTree
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import kotlin.math.roundToInt

class NewTreeViewModel(private val sharedPreferences: SharedPreferences,
                       private val userLocationManager: UserLocationManager) : ViewModel() {

    val noteEnabledLiveData: LiveData<Boolean> = MutableLiveData<Boolean>().apply {
        postValue(FeatureFlags.TREE_NOTE_FEATURE_ENABLED)
    }

    val accuracyLiveData: LiveData<Int> = MutableLiveData<Int>().apply {
        postValue(userLocationManager.currentLocation?.accuracy?.roundToInt() ?: 0)
    }


    fun generateNewTree(timeToNextUpdate: Int, note: String): NewTree? {
//        val minAccuracy = sharedPreferences.getInt(
//            ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
//            ValueHelper.MIN_ACCURACY_DEFAULT_SETTING
//        )
//
//        // note
//        val content = fragmentNewTreeNote.text.toString()
//
//        // tree
//        val planterInfoId = sharedPreferences.getLong(ValueHelper.PLANTER_INFO_ID, 0)
//        val planterCheckinId = sharedPreferences.getLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)
//
//        return currentPhotoPath?.let {
//            NewTree(
//                it,
//                minAccuracy,
//                timeToNextUpdate,
//                note,
//                planterCheckinId,
//                planterInfoId
//            )
//        }

        return null
    }

}