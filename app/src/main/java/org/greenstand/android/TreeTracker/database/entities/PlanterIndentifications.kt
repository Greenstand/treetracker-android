package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PlanterIndentifications.TABLE)
data class PlanterIndentifications(@PrimaryKey
                                   @ColumnInfo(name = ID)
                                   var id: Long,
                                   @ColumnInfo(name = PLANTER_DETAILS_ID)
                                   var planterDetailsId: Long?,
                                   @ColumnInfo(name = INDENTIFIER)
                                   var indentifier: String?,
                                   @ColumnInfo(name = PHOTO_PATH)
                                   var photoPath: String?,
                                   @ColumnInfo(name = PHOTO_URL)
                                   var photoUrl: String?,
                                   @ColumnInfo(name = TIME_CREATED)
                                   var timeCreated: Long?) {

    companion object {
        const val TABLE = "planter_identifications"
        const val ID = "_id"
        const val PLANTER_DETAILS_ID = "planter_details_id"
        const val INDENTIFIER = "identifier"
        const val PHOTO_PATH = "photo_path"
        const val PHOTO_URL = "photo_url"
        const val TIME_CREATED = "time_created"
    }
}