package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = PhotoEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = [LocationEntity.ID],
            childColumns = [PhotoEntity.LOCATION_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class PhotoEntity(
    @ColumnInfo(name = NAME)
    var name: String?,
    @ColumnInfo(name = LOCATION_ID)
    var locationId: Int?,
    @ColumnInfo(name = MAIN_DB_ID)
    var mainDbId: Int = 0,
    @ColumnInfo(name = IS_OUTDATED)
    var isOutdated: Int = 0,
    @ColumnInfo(name = TIME_TAKEN)
    var timeTaken: String,
    @ColumnInfo(name = USER_ID)
    var userId: Long?
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int = 0

    companion object {
        const val TABLE = "photo"
        const val ID = "_id"
        const val NAME = "name"
        const val LOCATION_ID = "location_id"
        const val MAIN_DB_ID = "main_db_id"
        const val IS_OUTDATED = "is_outdated"
        const val TIME_TAKEN = "time_taken"
        const val USER_ID = "user_id"
    }
}