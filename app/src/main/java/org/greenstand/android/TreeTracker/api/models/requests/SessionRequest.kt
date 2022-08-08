package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class SessionRequest(
    @SerializedName("id")
    val sessionId: String,
    @SerializedName("originating_wallet_registration_id")
    val originUserId: String,
    // Disabled temporarily
    //    @SerializedName("origin_wallet")
    //    val wallet: String,
    @SerializedName("target_wallet")
    val targetWallet: String,
    val organization: String,
    @SerializedName("device_configuration_id")
    val deviceConfigId: String,
)