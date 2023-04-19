/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import android.os.Parcelable
import java.util.UUID
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tree(
    val treeUuid: UUID,
    val sessionId: Long,
    val content: String,
    val photoPath: String,
    val meanLongitude: Double,
    val meanLatitude: Double,
    private val treeAttributes: MutableMap<String, String> = mutableMapOf()
) : Parcelable {

    fun addTreeAttribute(key: String, value: String) {
        treeAttributes[key] = value
    }

    fun treeCaptureAttributes(): Map<String, String> = treeAttributes

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
        const val ROTATION_MATRIX_KEY = "rotation_matrix"
        // DBH - Diameter at Breast Height (this is a standard method for measuring trees)
        const val DBH_ATTR_KEY = "dbh"
    }
}