package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = LocationEntity.TABLE)
data class LocationEntity(
    @ColumnInfo(name = ACCURACY)
    var accuracy: Int,
    @ColumnInfo(name = LAT)
    var latitude: Double?,
    @ColumnInfo(name = LONG)
    var longitude: Double?,
    @ColumnInfo(name = USER_ID)
    var userId: Long?,
    @ColumnInfo(name = MAIN_DB_ID)
    var mainDbId: Int = 0
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int = 0

    companion object {
        const val TABLE = "location"
        const val ID = "_id"
        const val ACCURACY = "accuracy"
        const val LAT = "lat"
        const val LONG = "long"
        const val USER_ID = "user_id"
        const val MAIN_DB_ID = "main_db_id"
    }
}