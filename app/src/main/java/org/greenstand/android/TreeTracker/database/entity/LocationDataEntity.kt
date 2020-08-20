package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = LocationDataEntity.TABLE
)
data class LocationDataEntity(
    @ColumnInfo(name = LocationDataEntity.BASE64_VALUE)
    var locationDataJson: String
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0
    @ColumnInfo(name = UPLOADED, index = true)
    var uploaded: Boolean = false
    @ColumnInfo(name = CREATED_AT)
    var createdAt: Long = System.currentTimeMillis()

    companion object {
        const val TABLE = "location_data"

        const val ID = "_id"
        // base64 encoded json of LocationData defined in LocationUpdateManager.kt
        const val BASE64_VALUE = "base64_json"
        const val UPLOADED = "uploaded"
        const val CREATED_AT = "created_at"
    }
}
