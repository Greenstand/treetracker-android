package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

data class NewTreeRequest(@SerializedName("user_id")
                          val userId: Int = 0,
                          @SerializedName("lat")
                          val lat: Double = 0.toDouble(),
                          @SerializedName("lon")
                          val lon: Double = 0.toDouble(),
                          @SerializedName("gps_accuracy")
                          val gpsAccuracy: Int = 0,
                          @SerializedName("note")
                          val note: String? = null,
                          @SerializedName("timestamp")
                          val timestamp: Long = 0,
                          @SerializedName("image_url")
                          val imageUrl: String? = null,
                          @SerializedName("sequence_id")
                          val sequenceId: Long = 0,
                          @SerializedName("planter_photo_url")
                          val planterPhotoUrl: String? = null,
                          @SerializedName("planter_identifier")
                          val planterIdentifier: String? = null,
                          @SerializedName("attributes")
                          val attributes: AttributesRequest? = null)

data class AttributesRequest(@SerializedName("height_color")
                             val heightColor: Int = 0,
                             @SerializedName("flavor_id")
                             val flavorId: Int = 0,
                             @SerializedName("app_version")
                             val appVersion: Int = 0,
                             @SerializedName("app_build")
                             val appBuild: Int = 0)
