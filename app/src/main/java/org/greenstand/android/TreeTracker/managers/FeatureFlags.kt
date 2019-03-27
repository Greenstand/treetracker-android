package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.BuildConfig

object FeatureFlags {
    const val TREE_HEIGHT_FEATURE_ENABLED = BuildConfig.TREE_HEIGHT_FEATURE_ENABLED == "true";
}