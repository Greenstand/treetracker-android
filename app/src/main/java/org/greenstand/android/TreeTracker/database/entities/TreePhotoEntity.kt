package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_photo")
data class TreePhotoEntity(@PrimaryKey
                           @ColumnInfo(name = "id")
                           var id: Long,
                           @ColumnInfo(name = "tree_id")
                           var treeId: Long,
                           @ColumnInfo(name = "photo_id")
                           var photoId: Long)