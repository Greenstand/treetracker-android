package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = LocationEntity.TABLE)
data class LocationEntity(@PrimaryKey
                          @ColumnInfo(name = ID)
                          var id: Long,
                          @ColumnInfo(name = ACCURACY)
                          var accuracy: Float,
                          @ColumnInfo(name = LAT)
                          var latitude: Double,
                          @ColumnInfo(name = LONG)
                          var longitude: Double) {

    companion object {
        const val TABLE = "location"
        const val ID = "_id"
        const val ACCURACY = "accuracy"
        const val LAT = "lat"
        const val LONG = "long"
    }
}