package org.greenstand.android.TreeTracker.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewTree(
    val photoPath: String,
    val content: String,
    val planterCheckInId: Long
) : Parcelable
