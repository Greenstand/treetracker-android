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
import kotlinx.serialization.Serializable
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

@Serializable
data class DeviceRequest(
    val device_identifier: String = DeviceUtils.deviceId,
    val app_version: String = BuildConfig.VERSION_NAME,
    val app_build: Int = BuildConfig.VERSION_CODE,
    val manufacturer: String = Build.MANUFACTURER,
    val brand: String = Build.BRAND,
    val model: String = Build.MODEL,
    val hardware: String = Build.HARDWARE,
    val device: String = Build.DEVICE,
    val serial: String = "unknown",
    val androidRelease: String = Build.VERSION.RELEASE,
    val androidSdkVersion: Int = Build.VERSION.SDK_INT,
    val instanceId: String,
)