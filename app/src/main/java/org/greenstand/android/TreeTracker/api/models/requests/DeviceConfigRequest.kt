package org.greenstand.android.TreeTracker.api.models.requests

import android.os.Build
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.annotations.SerializedName
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

class DeviceConfigRequest(
    @SerializedName("id")
    val id: String, //uuid
    @SerializedName("device_identifier")
    val deviceIdentifier: String = DeviceUtils.deviceId,
    @SerializedName("app_version")
    val appVersion: String,
    @SerializedName("app_build")
    val appBuild: Int,
    @SerializedName("manufacturer")
    val manufacturer: String = Build.MANUFACTURER,
    @SerializedName("brand")
    val brand: String = Build.BRAND,
    @SerializedName("model")
    val model: String = Build.MODEL,
    @SerializedName("hardware")
    val hardware: String = Build.HARDWARE,
    @SerializedName("device")
    val device: String = Build.DEVICE,
    @SerializedName("serial")
    val serial: String = Build.SERIAL,
    @SerializedName("os_version")
    val osVersion: String,
    @SerializedName("sdk_version")
    val sdkVersion: Int,
    @SerializedName("instance_id")
    val instanceId: String = FirebaseInstanceId.getInstance().id,
    @SerializedName("logged_at")
    val loggedAt: Long
)