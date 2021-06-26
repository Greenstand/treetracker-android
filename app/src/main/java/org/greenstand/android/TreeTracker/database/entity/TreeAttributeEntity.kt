package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = TreeAttributeEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = TreeCaptureEntity::class,
            parentColumns = [TreeCaptureEntity.ID],
            childColumns = [TreeAttributeEntity.TREE_CAPTURE_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreeAttributeEntity(
    @ColumnInfo(name = KEY)
    var key: String,
    @ColumnInfo(name = VALUE)
    var value: String,
    @ColumnInfo(name = TREE_CAPTURE_ID, index = true)
    var treeCaptureId: Long
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long = 0

    companion object {
        const val TABLE = "tree_attribute"
        const val ID = "_id"
        const val KEY = "key"
        const val VALUE = "value"
        const val TREE_CAPTURE_ID = "tree_capture_id"
    }
}