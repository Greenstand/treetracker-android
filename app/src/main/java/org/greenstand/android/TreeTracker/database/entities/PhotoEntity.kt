package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo")
data class PhotoEntity(@PrimaryKey
                       @ColumnInfo(name = "id")
                       var id: Long,
                       @ColumnInfo(name = "name")
                       var name: Short,
                       @ColumnInfo(name = "location_id")
                       var locationId: Long)