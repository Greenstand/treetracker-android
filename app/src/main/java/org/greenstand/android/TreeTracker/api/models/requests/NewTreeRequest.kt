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

@Serializable
data class NewTreeRequest(
    @SerialName("user_id")
    val userId: Int = 0,
    @SerialName("uuid")
    val uuid: String? = null,
    @SerialName("lat")
    val lat: Double = 0.toDouble(),
    @SerialName("lon")
    val lon: Double = 0.toDouble(),
    @SerialName("gps_accuracy")
    val gpsAccuracy: Int = 0,
    @SerialName("note")
    val note: String? = null,
    @SerialName("timestamp")
    val timestamp: Long = 0,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("sequence_id")
    val sequenceId: Long = 0,
    @SerialName("device_identifier")
    val deviceIdentifier: String? = null,
    @SerialName("planter_photo_url")
    val planterPhotoUrl: String? = null,
    @SerialName("planter_identifier")
    val planterIdentifier: String? = null,
    @SerialName("attributes")
    val attributes: List<AttributeRequest>? = null,
)

@Serializable
data class AttributeRequest(
    @SerialName("key")
    val key: String,
    @SerialName("value")
    val value: String,
)