package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.greenstand.android.TreeTracker.database.entity.SessionEntity.Companion.TABLE

@Entity(
    tableName = TABLE,
    foreignKeys = [
        ForeignKey(
            entity = PlanterInfoEntity::class,
            parentColumns = [PlanterInfoEntity.ID],
            childColumns = [SessionEntity.PLANTER_INFO_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    @ColumnInfo(name = RECORD_UUID)
    var uuid: Long,
    @ColumnInfo(name = PLANTER_INFO_ID, index = true)
    var planterInfoId: Long,
    @ColumnInfo(name = START_TIME)
    var startTime: Long,
    @ColumnInfo(name = END_TIME)
    var endTime: Long?,
    @ColumnInfo(name = TOTAL_PLANTED)
    var totalPlanted: Int?,
    @ColumnInfo(name = PLANTED_WITH_CONNECTION)
    var plantedWithConnection: Int?,
    @ColumnInfo(name = ORGANIZATION)
    var organization: String?,
    @ColumnInfo(name = WALLET)
    var wallet: String,
    @ColumnInfo(name = UPLOADED)
    var isUploaded: Boolean,
    @ColumnInfo(name = BUNDLE_ID)
    var bundleId: String? = null,
) {


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "session"

        const val ID = "_id"
        const val PLANTER_INFO_ID = "planter_info_id"
        const val ORGANIZATION = "organization"
        const val UPLOADED = "uploaded"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val RECORD_UUID = "record_uuid"
        const val TOTAL_PLANTED = "total_planted"
        const val PLANTED_WITH_CONNECTION = "planted_with_connection"
        const val WALLET = "wallet"
        const val BUNDLE_ID = "bundle_id"
    }
}