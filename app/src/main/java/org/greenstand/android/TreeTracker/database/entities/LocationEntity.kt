package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "location")
data class LocationEntity(@PrimaryKey
                          @ColumnInfo(name = "id")
                          var id: Long,
                          @ColumnInfo(name = "accuracy")
                          var accuracy: Float,
                          @ColumnInfo(name = "lat")
                          var latitude: Double,
                          @ColumnInfo(name = "long")
                          var longitude: Double)