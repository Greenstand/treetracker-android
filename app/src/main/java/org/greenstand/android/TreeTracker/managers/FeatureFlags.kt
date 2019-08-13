package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.BuildConfig

object FeatureFlags {
    val DEBUG_ENABLED: Boolean = BuildConfig.BUILD_TYPE == "dev" || BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "acceleration"
    val TREE_HEIGHT_FEATURE_ENABLED: Boolean = BuildConfig.TREE_HEIGHT_FEATURE_ENABLED
    val TREE_NOTE_FEATURE_ENABLED: Boolean = BuildConfig.TREE_NOTE_FEATURE_ENABLED
    val FABRIC_ENABLED: Boolean = BuildConfig.ENABLE_FABRIC
    val HIGH_GPS_ACCURACY: Boolean = BuildConfig.GPS_ACCURACY
    val AUTOMATIC_SIGN_OUT_FEATURE_ENABLED: Boolean = BuildConfig.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED
    val BLUR_DETECTION_ENABLED: Boolean = BuildConfig.BLUR_DETECTION_ENABLED
}