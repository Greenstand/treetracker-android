package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = GlobalSettingEntity.TABLE,
    foreignKeys = [
        ForeignKey(entity = SettingsEntity::class,
                   parentColumns = [SettingsEntity.ID],
                   childColumns = [GlobalSettingEntity.SETTING_ID],
                   onUpdate = ForeignKey.CASCADE)
    ]
)
class GlobalSettingEntity(@ColumnInfo(name = SETTING_ID)
                          var settingId: Long?) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null

    companion object {
        const val TABLE = "global_settings"
        const val ID = "_id"
        const val SETTING_ID = "setting_id"
    }
}
