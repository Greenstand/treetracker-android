package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = PlanterIndentificationsEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PlanterDetailsEntity::class,
            parentColumns = [PlanterDetailsEntity.ID],
            childColumns = [PlanterIndentificationsEntity.PLANTER_DETAILS_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class PlanterIndentificationsEntity(
    @ColumnInfo(name = PLANTER_DETAILS_ID)
    var planterDetailsId: Int?,
    @ColumnInfo(name = INDENTIFIER)
    var identifier: String?,
    @ColumnInfo(name = PHOTO_PATH)
    var photoPath: String?,
    @ColumnInfo(name = PHOTO_URL)
    var photoUrl: String?,
    @ColumnInfo(name = TIME_CREATED)
    var timeCreated: String
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int=0

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