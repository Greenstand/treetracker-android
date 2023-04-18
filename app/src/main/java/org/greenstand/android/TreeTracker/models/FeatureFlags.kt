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

import org.greenstand.android.TreeTracker.BuildConfig

object FeatureFlags {
    val DEBUG_ENABLED: Boolean = (
        BuildConfig.BUILD_TYPE == "dev" ||
            BuildConfig.BUILD_TYPE == "debug"
        )
    val BETA: Boolean = BuildConfig.BUILD_TYPE == "beta"
    val TREE_HEIGHT_FEATURE_ENABLED: Boolean = BuildConfig.TREE_HEIGHT_FEATURE_ENABLED
    val TREE_NOTE_FEATURE_ENABLED: Boolean = BuildConfig.TREE_NOTE_FEATURE_ENABLED
    val TREE_DBH_FEATURE_ENABLED: Boolean = BuildConfig.TREE_DBH_FEATURE_ENABLED
    val AUTOMATIC_SIGN_OUT_FEATURE_ENABLED: Boolean = BuildConfig.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED
    val BLUR_DETECTION_ENABLED: Boolean = BuildConfig.BLUR_DETECTION_ENABLED
    val ORG_LINK_ENABLED: Boolean = BuildConfig.ORG_LINK
}
