package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = SettingsEntity.TABLE)
data class SettingsEntity(
    @ColumnInfo(name = MAIN_DB_ID)
    var mainDbId: Int = 0,
    @ColumnInfo(name = TIME_TO_NEXT_UPDATE)
    var timeToNextUpdate: Int = 60,
    @ColumnInfo(name = MIN_ACCURACY)
    var minAccuracy: Int = 200
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int = 0

    companion object {
        const val TABLE = "settings"
        const val ID = "_id"
        const val MAIN_DB_ID = "main_db_id"
        const val TIME_TO_NEXT_UPDATE = "time_to_next_update"
        const val MIN_ACCURACY = "min_accuracy"
    }
}