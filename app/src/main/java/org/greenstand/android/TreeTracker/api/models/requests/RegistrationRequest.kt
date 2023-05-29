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

import com.google.gson.annotations.SerializedName
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

data class RegistrationRequest(
    @SerializedName("planter_identifier")
    val planterIdentifier: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("organization")
    val organization: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("lat")
    val lat: Double?,
    @SerializedName("lon")
    val lon: Double?,
    @SerializedName("device_identifier")
    val deviceIdentifier: String = DeviceUtils.deviceId,
    @SerializedName("record_uuid")
    val recordUuid: String,
    @SerializedName("image_url")
    val imageUrl: String,
)