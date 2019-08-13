package org.greenstand.android.TreeTracker.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewTree(val photoPath: String,
                   val minAccuracy: Int,
                   val timeToNextUpdate: Int,
                   val content: String,
                   val planterCheckInId: Long,
                   val planterInfoId: Long) : Parcelable