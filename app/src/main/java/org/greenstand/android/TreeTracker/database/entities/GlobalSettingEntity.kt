package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = GlobalSettingEntity.TABLE)
class GlobalSettingEntity(@ColumnInfo(name = SETTING_ID)
                          var settingId: Long) {

    companion object {
        const val TABLE = "global_settings"
        const val SETTING_ID = "setting_id"
    }
}
