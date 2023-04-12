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

class TreeCaptureRequest(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("id")
    val treeId: String,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lon")
    val lon: Double,
    @SerializedName("note")
    val note: String?,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("captured_at")
    val createdAt: String,
    @SerializedName("abs_step_count")
    val stepCount: Long?,
    @SerializedName("delta_step_count")
    val deltaStepCount: Long?,
    @SerializedName("rotation_matrix")
    val rotationMatrix: String?,
    @SerializedName("extra_attributes")
    val extraAttributes: String?,
)