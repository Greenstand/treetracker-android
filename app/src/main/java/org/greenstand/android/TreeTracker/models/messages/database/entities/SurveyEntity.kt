package org.greenstand.android.TreeTracker.models.messages.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "surveys",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
class SurveyEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "message_id", index = true)
    val messageId: String,
    @ColumnInfo(name = "title")
    val title: String,
)