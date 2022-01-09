package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "location",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["session_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class LocationEntity(
    @ColumnInfo(name = "json_value")
    var locationDataJson: String,
    @ColumnInfo(name = "session_id", index = true)
    var sessionId: Long,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
    @ColumnInfo(name = "uploaded", index = true)
    var uploaded: Boolean = false
    @ColumnInfo(name = "create_at")
    var createdAt: Long = System.currentTimeMillis()
}