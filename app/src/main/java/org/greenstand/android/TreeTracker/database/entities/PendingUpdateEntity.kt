package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = PendingUpdateEntity.TABLE,
    foreignKeys = [
        ForeignKey(entity = SettingsEntity::class,
                   parentColumns = [SettingsEntity.ID],
                   childColumns = [PendingUpdateEntity.SETTINGS_ID],
                   onUpdate = ForeignKey.CASCADE),
        ForeignKey(entity = TreeEntity::class,
                   parentColumns = [TreeEntity.ID],
                   childColumns = [PendingUpdateEntity.TREE_ID],
                   onUpdate = ForeignKey.CASCADE),
        ForeignKey(entity = LocationEntity::class,
                   parentColumns = [LocationEntity.ID],
                   childColumns = [PendingUpdateEntity.LOCATION_ID],
                   onUpdate = ForeignKey.CASCADE)
    ]
)
data class PendingUpdateEntity(@ColumnInfo(name = MAIN_DB_ID)
                               var mainDbId: Long?,
                               @ColumnInfo(name = USER_ID)
                               var userId: Long?,
                               @ColumnInfo(name = SETTINGS_ID)
                               var settingsId: Long?,
                               @ColumnInfo(name = TREE_ID)
                               var treeId: Long?,
                               @ColumnInfo(name = LOCATION_ID)
                               var locationId: Long?,
                               @ColumnInfo(name = RADIUS)
                               var radius: Long?) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null

    companion object {
        const val TABLE = "pending_updates"
        const val ID = "_id"
        const val MAIN_DB_ID = "main_db_id"
        const val USER_ID = "user_id"
        const val SETTINGS_ID = "settings_id"
        const val TREE_ID = "tree_id"
        const val LOCATION_ID = "location_id"
        const val RADIUS = "radius"
    }
}