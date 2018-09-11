package org.greenstand.android.TreeTracker.api.models.requests

import com.google.gson.annotations.SerializedName

class DeviceRequest {

    @SerializedName("app_version")
    var app_version: String? = null

    @SerializedName("app_build")
    var app_build: Int? = null

    @SerializedName("manufacturer")
    var manufacturer: String? = null

    @SerializedName("brand")
    var brand: String? = null

    @SerializedName("model")
    var model: String? = null

    @SerializedName("hardware")
    var hardware: String? = null

    @SerializedName("device")
    var device: String? = null

    @SerializedName("serial")
    var serial: String? = null

    @SerializedName("androidRelease")
    var androidRelease: String? = null

    @SerializedName("androidSdkVersion")
    var androidSdkVersion: Int? = null

}