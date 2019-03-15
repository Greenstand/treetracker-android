package org.greenstand.android.TreeTracker.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = SqliteSequenceEntity.TABLE)
class SqliteSequenceEntity(@PrimaryKey
                           @ColumnInfo(name = ID)
                           var id: Long,
                           @ColumnInfo(name = NAME)
                           var name: String?,
                           @ColumnInfo(name = SEQ)
                           var seq: Int?) {

    companion object {
        const val TABLE = "sqlite_sequence"
        const val ID = "_id"
        const val NAME = "name"
        const val SEQ = "seq"
    }
}