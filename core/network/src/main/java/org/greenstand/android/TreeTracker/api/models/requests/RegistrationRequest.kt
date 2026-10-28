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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.greenstand.android.TreeTracker.utilities.DeviceUtils

@Serializable
data class RegistrationRequest(
    @SerialName("planter_identifier")
    val planterIdentifier: String?,
    @SerialName("first_name")
    val firstName: String?,
    @SerialName("last_name")
    val lastName: String?,
    @SerialName("organization")
    val organization: String?,
    @SerialName("phone")
    val phone: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("lat")
    val lat: Double?,
    @SerialName("lon")
    val lon: Double?,
    @SerialName("device_identifier")
    val deviceIdentifier: String = DeviceUtils.deviceId,
    @SerialName("record_uuid")
    val recordUuid: String,
    @SerialName("image_url")
    val imageUrl: String,
)