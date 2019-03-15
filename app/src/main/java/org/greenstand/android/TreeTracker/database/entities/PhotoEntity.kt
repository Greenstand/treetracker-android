package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PhotoEntity.TABLE)
data class PhotoEntity(@PrimaryKey
                       @ColumnInfo(name = ID)
                       var id: Long,
                       @ColumnInfo(name = NAME)
                       var name: Short,
                       @ColumnInfo(name = LOCATION_ID)
                       var locationId: Long) {

    companion object {
        const val TABLE = "photo"
        const val ID = "_id"
        const val NAME = "name"
        const val LOCATION_ID = "location_Id"
    }
}