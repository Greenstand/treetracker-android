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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

@Serializable
class DeviceConfigRequest(
    @SerialName("id")
    val id: String, // uuid
    @SerialName("device_identifier")
    val deviceIdentifier: String = DeviceUtils.deviceId,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("app_build")
    val appBuild: Int,
    @SerialName("manufacturer")
    val manufacturer: String = Build.MANUFACTURER,
    @SerialName("brand")
    val brand: String = Build.BRAND,
    @SerialName("model")
    val model: String = Build.MODEL,
    @SerialName("hardware")
    val hardware: String = Build.HARDWARE,
    @SerialName("device")
    val device: String = Build.DEVICE,
    @SerialName("serial")
    val serial: String = "unknown",
    @SerialName("os_version")
    val osVersion: String,
    @SerialName("sdk_version")
    val sdkVersion: Int,
    @SerialName("instance_id")
    val instanceId: String,
    @SerialName("logged_at")
    val loggedAt: String,
)