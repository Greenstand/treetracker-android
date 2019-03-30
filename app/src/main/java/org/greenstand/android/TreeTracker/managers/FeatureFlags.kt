package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.BuildConfig

object FeatureFlags {
    val TREE_HEIGHT_FEATURE_ENABLED: Boolean = BuildConfig.TREE_HEIGHT_FEATURE_ENABLED
    val FABRIC_ENABLED: Boolean = BuildConfig.ENABLE_FABRIC
    val HIGH_GPS_ACCURACY: Boolean = BuildConfig.GPS_ACCURACY
}