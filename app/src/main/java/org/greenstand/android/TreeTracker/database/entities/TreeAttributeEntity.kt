package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_attributes")
data class TreeAttributeEntity(@PrimaryKey
                               @ColumnInfo(name = "id")
                               var id: Long,
                               @ColumnInfo(name = "tree_id")
                               var treeId: Long,
                               @ColumnInfo(name = "height_color")
                               var heightColor: String,
                               @ColumnInfo(name = "flavor_id")
                               var flavorId: String,
                               @ColumnInfo(name = "app_version")
                               var appVersion: String,
                               @ColumnInfo(name = "app_build")
                               var appBuild: String)