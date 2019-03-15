package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TreeNoteEntity.TABLE)
data class TreeNoteEntity(@PrimaryKey
                          @ColumnInfo(name = ID)
                          var id: Long,
                          @ColumnInfo(name = NOTE_ID)
                          var noteId: Long,
                          @ColumnInfo(name = TREE_ID)
                          var treeId: Long) {

    companion object {
        const val TABLE = "tree_note"
        const val ID = "_id"
        const val NOTE_ID = "note_id"
        const val TREE_ID = "tree_id"
    }
}