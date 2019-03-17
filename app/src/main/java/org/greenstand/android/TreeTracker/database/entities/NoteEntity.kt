package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = NoteEntity.TABLE)
data class NoteEntity(
    @ColumnInfo(name = MAIN_DB_ID)
    var mainDbId: Int = 0,
    @ColumnInfo(name = CONTENT)
    var content: String?,
    @ColumnInfo(name = TIME_CREATED)
    var timeCreated: String,
    @ColumnInfo(name = USER_ID)
    var userId: Long?
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int = 0

    companion object {
        const val TABLE = "note"
        const val ID = "_id"
        const val MAIN_DB_ID = "main_db_id"
        const val CONTENT = "content"
        const val TIME_CREATED = "time_created"
        const val USER_ID = "user_id"
    }
}