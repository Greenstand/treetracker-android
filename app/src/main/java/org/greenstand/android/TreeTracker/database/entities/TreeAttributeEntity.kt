package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TreeAttributeEntity.TABLE)
data class TreeAttributeEntity(@PrimaryKey
                               @ColumnInfo(name = ID)
                               var id: Long,
                               @ColumnInfo(name = TREE_ID)
                               var treeId: Long,
                               @ColumnInfo(name = HEIGHT_COLOR)
                               var heightColor: String,
                               @ColumnInfo(name = FLAVOR_ID)
                               var flavorId: String,
                               @ColumnInfo(name = APP_VERSION)
                               var appVersion: String,
                               @ColumnInfo(name = APP_BUILD)
                               var appBuild: String) {

    companion object {
        const val TABLE = "tree_attributes"
        const val ID = "_id"
        const val TREE_ID = "tree_id"
        const val HEIGHT_COLOR = "height_color"
        const val FLAVOR_ID = "flavor_id"
        const val APP_VERSION = "app_version"
        const val APP_BUILD = "app_build"
    }
}