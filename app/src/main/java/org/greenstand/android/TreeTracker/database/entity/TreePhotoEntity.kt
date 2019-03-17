package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = TreePhotoEntity.TABLE,
    primaryKeys = [TreePhotoEntity.TREE_ID, TreePhotoEntity.PHOTO_ID],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = [PhotoEntity.ID],
            childColumns = [TreePhotoEntity.PHOTO_ID],
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TreeEntity::class,
            parentColumns = [TreeEntity.ID],
            childColumns = [TreePhotoEntity.TREE_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreePhotoEntity(
    @ColumnInfo(name = TREE_ID)
    var treeId: Long,
    @ColumnInfo(name = PHOTO_ID)
    var photoId: Long
) {

    companion object {
        const val TABLE = "tree_photo"
        const val TREE_ID = "tree_id"
        const val PHOTO_ID = "photo_id"
    }
}