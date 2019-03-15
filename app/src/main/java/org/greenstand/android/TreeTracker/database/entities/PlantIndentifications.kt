package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PlantIndentifications.TABLE)
data class PlantIndentifications(@PrimaryKey
                                 @ColumnInfo(name = ID)
                                 var id: Long,
                                 @ColumnInfo(name = PLANT_DETAILS_ID)
                                 var planDetailsId: Long,
                                 @ColumnInfo(name = INDENTIFIER)
                                 var indentifier: String,
                                 @ColumnInfo(name = PHOTO_PATH)
                                 var photoPath: String,
                                 @ColumnInfo(name = PHOTO_URL)
                                 var photoUrl: String,
                                 @ColumnInfo(name = TIME_CREATED)
                                 var timeCreated: Long) {

    companion object {
        const val TABLE = "plant_indentifications"
        const val ID = "_id"
        const val PLANT_DETAILS_ID = "_id"
        const val INDENTIFIER = "_id"
        const val PHOTO_PATH = "_id"
        const val PHOTO_URL = "_id"
        const val TIME_CREATED = "_id"
    }
}