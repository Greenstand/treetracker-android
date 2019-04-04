package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = TreeAttributesEntity.TABLE,
    foreignKeys = [
        ForeignKey(
            entity = TreeEntity::class,
            parentColumns = [TreeEntity.ID],
            childColumns = [TreeAttributesEntity.TREE_ID],
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class TreeAttributesEntity(@ColumnInfo(name = KEY)
                                var key: String,
                                @ColumnInfo(name = VALUE)
                                var value: String,
                                @ColumnInfo(name = TREE_ID)
                                var treeId: Long) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Long? = null

    companion object {
        const val TABLE = "tree_attributes"
        const val ID = "_id"
        const val KEY = "key"
        const val VALUE = "value"
        const val TREE_ID = "tree_id"
    }
}