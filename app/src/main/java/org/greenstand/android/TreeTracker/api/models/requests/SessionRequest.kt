package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class SessionRequest(
    @SerializedName("id")
    val sessionId: String,
    val wallet: String,
    @SerializedName("target_wallet")
    val targetWallet: String,
    val organization: String,
    @SerializedName("device_configuration_id")
    val deviceConfigId: String,
)