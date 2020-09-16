package org.greenstand.android.TreeTracker.models

import android.os.Parcelable
import java.util.UUID
import kotlinx.android.parcel.Parcelize

@Parcelize
class Tree(
    val treeUuid: UUID,
    val planterCheckInId: Long,
    val content: String,
    val photoPath: String
) : Parcelable {

    private val treeCaptureAttributes = mutableMapOf<String, String>()

    fun addTreeAttribute(key: String, value: String) {
        treeCaptureAttributes[key] = value
    }

    fun treeCaptureAttributes(): Map<String, String> = treeCaptureAttributes

    companion object Attributes {
        const val TREE_COLOR_ATTR_KEY = "height_color"
        const val APP_BUILD_ATTR_KEY = "app_build"
        const val APP_FLAVOR_ATTR_KEY = "app_flavor"
        const val APP_VERSION_ATTR_KEY = "app_version"
        // Refers to the absolute step count since the device is rebooted
        const val ABS_STEP_COUNT_KEY = "abs_step_count"
        // Delta step count is the difference between the absolute count at the time of capturing
        // a tree minus the last absolute step count recorded when capturing a previous tree. This
        // is the indicator for the number of steps taken between two trees.
        const val DELTA_STEP_COUNT_KEY = "delta_step_count"
    }
}
