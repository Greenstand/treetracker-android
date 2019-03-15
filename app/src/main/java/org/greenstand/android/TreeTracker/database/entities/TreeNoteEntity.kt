package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_note")
data class TreeNoteEntity(@PrimaryKey
                          @ColumnInfo(name = "id")
                          var id: Long,
                          @ColumnInfo(name = "note_id")
                          var noteId: Long,
                          @ColumnInfo(name = "tree_id")
                          var treeId: Long)