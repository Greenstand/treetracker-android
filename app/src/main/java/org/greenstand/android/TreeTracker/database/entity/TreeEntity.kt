package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tree",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["_id"],
            childColumns = ["session_id"],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreeEntity(
    @ColumnInfo(name = "uuid")
    var uuid: String,
    @ColumnInfo(name = "session_id", index = true)
    var sessionId: Long,
    @ColumnInfo(name = "photo_path")
    var photoPath: String?,
    @ColumnInfo(name = "photo_url")
    var photoUrl: String?,
    @ColumnInfo(name = "note")
    var note: String,
    @ColumnInfo(name = "latitude")
    var latitude: Double,
    @ColumnInfo(name = "longitude")
    var longitude: Double,
    @ColumnInfo(name = "uploaded", index = true)
    var uploaded: Boolean = false,
    @ColumnInfo(name = "created_at")
    var createdAt: Long,
    @ColumnInfo(name = "bundle_id", defaultValue = "NULL")
    var bundleId: String? = null,
    @ColumnInfo(name = "extra_attributes", defaultValue = "NULL")
    var extraAttributes: Map<String, String>? = null,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}