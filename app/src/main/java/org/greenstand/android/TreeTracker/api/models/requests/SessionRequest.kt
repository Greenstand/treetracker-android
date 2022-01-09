package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class SessionRequest(
    @SerializedName("session_id")
    val sessionId: String,
    val wallet: String,
    @SerializedName("target_wallet")
    val targetWallet: String,
    val organization: String,
)