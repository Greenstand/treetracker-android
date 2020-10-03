package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class NewTreeRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("gps_accuracy")
    val gpsAccuracy: Int,
    @SerializedName("note")
    val note: String?,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("sequence_id")
    val sequenceId: Long,
    @SerializedName("device_identifier")
    val deviceIdentifier: String,
    @SerializedName("planter_photo_url")
    val planterPhotoUrl: String?,
    @SerializedName("planter_identifier")
    val planterIdentifier: String,
    @SerializedName("attributes")
    val attributes: List<AttributeRequest>?
)

data class AttributeRequest(
    @SerializedName("key")
     val key: String,
    @SerializedName("value")
    val value: String
)
