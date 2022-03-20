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
    val USE_SWAHILI: Boolean = BuildConfig.FLAVOR != "greenstand"
}
