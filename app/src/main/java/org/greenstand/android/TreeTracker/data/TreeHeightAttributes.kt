package org.greenstand.android.TreeTracker.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.greenstand.android.TreeTracker.BuildConfig

@Parcelize
data class TreeHeightAttributes(
    val heightColor: TreeColor,
    val appFlavor: String = BuildConfig.FLAVOR,
    val appBuild: String = BuildConfig.VERSION_NAME,
    val appVersion: String = BuildConfig.VERSION_CODE.toString()
) : Parcelable
