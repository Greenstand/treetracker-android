package org.greenstand.android.TreeTracker.api.models.responses

import com.google.gson.annotations.SerializedName

data class UserTree(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("created")
    val created: String? = null,
    @SerializedName("updated")
    val updated: String? = null,
    @SerializedName("priority")
    val priority: String? = null,
    @SerializedName("lat")
    val lat: String? = null,
    @SerializedName("lng")
    val lng: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null
)

data class PlanterAccountData(
    @SerializedName("planterId")
    val planterIdentifier: String,
    @SerializedName("uploadedTrees")
    val uploadedTrees: Int,
    @SerializedName("validatedTrees")
    val validatedTrees: Int,
    @SerializedName("totalPayments")
    val totalPayments: Double,
    @SerializedName("pendingPayments")
    val pendingPayments: Double
)
