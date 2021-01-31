package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class TransferRequest(
    @SerializedName("uuid")
    val treeUUid: String,
    @SerializedName("receiverWallet")
    val wallet: String
)