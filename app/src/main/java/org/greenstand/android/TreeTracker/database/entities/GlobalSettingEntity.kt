package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = GlobalSettingEntity.TABLE)
class GlobalSettingEntity(@PrimaryKey
                          @ColumnInfo(name = ID)
                          var id: Long,
                          @ColumnInfo(name = SETTING_ID)
                          var settingId: Long?) {

    companion object {
        const val TABLE = "global_settings"
        const val ID = "_id"
        const val SETTING_ID = "setting_id"
    }
}
