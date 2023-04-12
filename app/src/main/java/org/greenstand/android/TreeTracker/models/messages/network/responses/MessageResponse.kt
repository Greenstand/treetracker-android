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
package org.greenstand.android.TreeTracker.models.messages.network.responses

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    val id: String,
    val type: MessageType,
    val from: String,
    val to: String,
    val subject: String?,
    val body: String?,
    @SerializedName("composed_at")
    val composedAt: String,
    @SerializedName("parent_message_id")
    val parentMessageId: String?,
    @SerializedName("video_link")
    val videoLink: String?,
    @SerializedName("survey_response")
    val surveyResponse: List<String>?,
    val survey: SurveyResponse?,
)