package org.greenstand.android.TreeTracker.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = TreeNoteEntity.TABLE,
    primaryKeys = [TreeNoteEntity.NOTE_ID, TreeNoteEntity.TREE_ID],
    foreignKeys = [
        ForeignKey(entity = NoteEntity::class,
                   parentColumns = [NoteEntity.ID],
                   childColumns = [TreeNoteEntity.NOTE_ID],
                   onUpdate = ForeignKey.CASCADE),
        ForeignKey(entity = TreeEntity::class,
                   parentColumns = [TreeEntity.ID],
                   childColumns = [TreeNoteEntity.TREE_ID],
                   onUpdate = ForeignKey.CASCADE)
    ]
)
data class TreeNoteEntity(@ColumnInfo(name = NOTE_ID)
                          var noteId: Long,
                          @ColumnInfo(name = TREE_ID)
                          var treeId: Long) {

    companion object {
        const val TABLE = "tree_note"
        const val NOTE_ID = "note_id"
        const val TREE_ID = "tree_id"
    }
}