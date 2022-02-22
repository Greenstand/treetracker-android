package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

class TreeCaptureRequest(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("capture_id")
    val treeId: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("note")
    val note: String?,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("abs_step_count")
    val stepCount: Long?,
    @SerializedName("delta_step_count")
    val deltaStepCount: Long?,
    @SerializedName("rotation_matrix")
    val rotationMatrix: String?,
    @SerializedName("extra_attributes")
    val extraData: String,
)