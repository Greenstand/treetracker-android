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
)