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
class TreeCaptureRequest(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("id")
    val treeId: String,
    @SerialName("lat")
    val lat: Double,
    @SerialName("lon")
    val lon: Double,
    @SerialName("note")
    val note: String?,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("captured_at")
    val createdAt: String,
    @SerialName("abs_step_count")
    val stepCount: Long?,
    @SerialName("delta_step_count")
    val deltaStepCount: Long?,
    @SerialName("rotation_matrix")
    val rotationMatrix: String?,
    @SerialName("extra_attributes")
    val extraAttributes: String?,
)