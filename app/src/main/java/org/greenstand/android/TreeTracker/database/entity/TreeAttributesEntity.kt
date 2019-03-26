package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TreeAttributesEntity.TABLE)
data class TreeAttributesEntity(@ColumnInfo(name = HEIGHT_COLOR)
                                var heightColor: String?,
                                @ColumnInfo(name = FLAVOR_ID)
                                var flavorId: String,
                                @ColumnInfo(name = APP_VERSION)
                                var appVersion: String,
                                @ColumnInfo(name = APP_BUILD)
                                var appBuild: String) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null

    companion object {
        const val TABLE = "tree_attributes"
        const val ID = "_id"
        const val HEIGHT_COLOR = "height_color"
        const val FLAVOR_ID = "flavor_id"
        const val APP_VERSION = "app_version"
        const val APP_BUILD = "app_build"
    }
}