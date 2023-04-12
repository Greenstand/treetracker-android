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
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

data class DeviceRequest(
    @SerializedName("device_identifier")
    val device_identifier: String = DeviceUtils.deviceId,
    @SerializedName("app_version")
    val app_version: String = BuildConfig.VERSION_NAME,
    @SerializedName("app_build")
    val app_build: Int = BuildConfig.VERSION_CODE,
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
    @SerializedName("androidRelease")
    val androidRelease: String = Build.VERSION.RELEASE,
    @SerializedName("androidSdkVersion")
    val androidSdkVersion: Int = Build.VERSION.SDK_INT,
    @SerializedName("instanceId")
    val instanceId: String = FirebaseInstanceId.getInstance().id
)
