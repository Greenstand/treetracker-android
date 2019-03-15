package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TreePhotoEntity.TABLE)
data class TreePhotoEntity(@PrimaryKey
                           @ColumnInfo(name = ID)
                           var id: Long,
                           @ColumnInfo(name = TREE_ID)
                           var treeId: Long?,
                           @ColumnInfo(name = PHOTO_ID)
                           var photoId: Long?) {

    companion object {
        const val TABLE = "tree_photo"
        const val ID = "_id"
        const val TREE_ID = "tree_id"
        const val PHOTO_ID = "photo_id"
    }
}