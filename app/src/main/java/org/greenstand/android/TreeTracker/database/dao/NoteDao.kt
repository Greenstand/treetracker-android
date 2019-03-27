package org.greenstand.android.TreeTracker.database.dao

import androidx.room.*
import org.greenstand.android.TreeTracker.database.entity.NoteEntity
import org.greenstand.android.TreeTracker.database.entity.TreeNoteEntity

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(note: TreeNoteEntity): Long

    @Transaction
    @Query(
        "select tree_id, note.* from tree left outer join tree_note on tree_id = tree._id left outer join note on note_id = note._id where content is not null and tree_id = :treeIdStr order by note.time_created asc"
    )
    fun getNotesByTreeID(treeIdStr: String): List<NoteEntity>

}