package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PhotoEntity.TABLE)
data class PhotoEntity(@PrimaryKey
                       @ColumnInfo(name = ID)
                       var id: Long,
                       @ColumnInfo(name = NAME)
                       var name: String?,
                       @ColumnInfo(name = LOCATION_ID)
                       var locationId: Long?,
                       @ColumnInfo(name = MAIN_DB_ID)
                       var mainDbId: Long?,
                       @ColumnInfo(name = IS_OUTDATED)
                       var isOutdated: Long?,
                       @ColumnInfo(name = TIME_TAKEN)
                       var timeTaken: Long?,
                       @ColumnInfo(name = USER_ID)
                       var userId: Long?) {

    companion object {
        const val TABLE = "photo"
        const val ID = "_id"
        const val NAME = "name"
        const val LOCATION_ID = "location_Id"
        const val MAIN_DB_ID = "main_db_id"
        const val IS_OUTDATED = "is_outdated"
        const val TIME_TAKEN = "time_taken"
        const val USER_ID = "user_id"
    }
}