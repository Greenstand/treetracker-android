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
package org.greenstand.android.TreeTracker.models.messages.network.requests

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    val id: String,
    val parentMessageId: String?,
    @SerializedName("recipient_handle")
    val recipientHandle: String,
    @SerializedName("author_handle")
    val authorHandle: String,
    val type: String,
    val body: String?,
    @SerializedName("survey_response")
    val surveyResponse: List<String>?,
    @SerializedName("survey_id")
    val surveyId: String?,
    @SerializedName("composed_at")
    val composedAt: String,
)