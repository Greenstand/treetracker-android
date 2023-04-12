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
package org.greenstand.android.TreeTracker.models.messages.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.greenstand.android.TreeTracker.models.messages.network.responses.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "wallet")
    val wallet: String,
    @ColumnInfo(name = "type")
    val type: MessageType,
    @ColumnInfo(name = "from")
    val from: String,
    @ColumnInfo(name = "to")
    val to: String,
    @ColumnInfo(name = "subject")
    val subject: String?,
    @ColumnInfo(name = "body")
    val body: String?,
    @ColumnInfo(name = "composed_at")
    val composedAt: String,
    @ColumnInfo(name = "parent_message_id")
    val parentMessageId: String?,
    @ColumnInfo(name = "video_link")
    val videoLink: String?,
    @ColumnInfo(name = "survey_response")
    val surveyResponse: List<String>?,
    @ColumnInfo(name = "should_upload")
    val shouldUpload: Boolean,
    @ColumnInfo(name = "bundle_id")
    val bundleId: String?,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean,
    @ColumnInfo(name = "survey_id", index = true)
    val surveyId: String?,
    @ColumnInfo(name = "is_survey_complete")
    val isSurveyComplete: Boolean?,
)