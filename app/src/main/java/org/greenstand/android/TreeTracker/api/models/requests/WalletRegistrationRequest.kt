package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class WalletRegistrationRequest(
    @SerializedName("id")
    val registrationId: String,
    @SerializedName("wallet")
    val wallet: String,
    @SerializedName("user_photo_url")
    val imageUrl: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("registered_at")
    val createdAt: Long,
)