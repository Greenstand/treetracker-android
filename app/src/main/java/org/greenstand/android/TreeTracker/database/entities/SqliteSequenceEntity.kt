package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = SqliteSequenceEntity.TABLE)
class SqliteSequenceEntity(@ColumnInfo(name = NAME)
                           var name: String,
                           @ColumnInfo(name = SEQ)
                           var seq: Int) {

    companion object {
        const val TABLE = "sqlite_sequence"
        const val NAME = "name"
        const val SEQ = "seq"
    }
}