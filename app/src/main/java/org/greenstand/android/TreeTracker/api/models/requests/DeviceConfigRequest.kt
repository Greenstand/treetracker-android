/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.api.models.requests

import android.os.Build
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.annotations.SerializedName
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
    val loggedAt: String
)