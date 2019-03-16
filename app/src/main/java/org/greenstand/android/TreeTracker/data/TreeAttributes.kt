package org.greenstand.android.TreeTracker.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TreeAttributes(val heightColor: TreeColor,
                          val appFlavor: String,
                          val appBuild: String): Parcelable