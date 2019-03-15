package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = SettingsEntity.TABLE)
data class SettingsEntity(@PrimaryKey
                          @ColumnInfo(name = ID)
                          var id: Long,
                          @ColumnInfo(name = MAIN_DB_ID)
                          var mainDbId: Long?,
                          @ColumnInfo(name = TIME_TO_NEXT_UPDATE)
                          var timeToNextUpdate: Long?,
                          @ColumnInfo(name = MIN_ACCURACY)
                          var minAccuracy: Int?) {

    companion object {
        const val TABLE = "settings"
        const val ID = "_id"
        const val MAIN_DB_ID = "main_db_id"
        const val TIME_TO_NEXT_UPDATE = "time_to_next_update"
        const val MIN_ACCURACY = "min_accuracy"
    }
}