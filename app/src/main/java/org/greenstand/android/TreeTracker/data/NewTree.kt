package org.greenstand.android.TreeTracker.data

import android.os.Parcelable
import java.util.UUID
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NewTree(
    val photoPath: String,
    val content: String,
    val planterCheckInId: Long,
    val treeUuid: UUID
) : Parcelable
