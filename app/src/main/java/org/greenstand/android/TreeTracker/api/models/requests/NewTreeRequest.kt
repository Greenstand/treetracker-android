package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

class NewTreeRequest {
    @SerializedName("user_id")
    var userId: Int = 0
    @SerializedName("lat")
    var lat: Double = 0.toDouble()
    @SerializedName("lon")
    var lon: Double = 0.toDouble()
    @SerializedName("gps_accuracy")
    private var gpsAccuracy: Int = 0
    @SerializedName("note")
    var note: String? = null
    @SerializedName("timestamp")
    var timestamp: Long = 0
    @SerializedName("image_url")
    var imageUrl: String? = null
    @SerializedName("sequence_id")
    var sequenceId: Long = 0
    @SerializedName("planter_photo_url")
    var planterPhotoUrl: String? = null
    @SerializedName("planter_identifier")
    var planterIdentifier: String? = null

    fun getGpsAccuracy(): Float {
        return gpsAccuracy.toFloat()
    }

    fun setGpsAccuracy(gpsAccuracy: Int) {
        this.gpsAccuracy = gpsAccuracy
    }
}
