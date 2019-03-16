package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = TreePhotoEntity.TABLE,
    foreignKeys = [
        ForeignKey(entity = PhotoEntity::class,
                   parentColumns = [PhotoEntity.ID],
                   childColumns = [TreePhotoEntity.PHOTO_ID],
                   onUpdate = ForeignKey.CASCADE),
        ForeignKey(entity = TreeEntity::class,
                   parentColumns = [TreeEntity.ID],
                   childColumns = [TreePhotoEntity.TREE_ID],
                   onUpdate = ForeignKey.CASCADE)
    ]
)
data class TreePhotoEntity(@ColumnInfo(name = TREE_ID)
                           var treeId: Long?,
                           @ColumnInfo(name = PHOTO_ID)
                           var photoId: Long?) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null

    companion object {
        const val TABLE = "tree_photo"
        const val ID = "_id"
        const val TREE_ID = "tree_id"
        const val PHOTO_ID = "photo_id"
    }
}