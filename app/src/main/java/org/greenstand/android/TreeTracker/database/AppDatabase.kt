package org.greenstand.android.TreeTracker.database

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.greenstand.android.TreeTracker.database.entities.*
import androidx.room.Room



@Database(entities = [
    //TreeAttributeEntity::class,
    TreeEntity::class,
    TreeNoteEntity::class,
    TreePhotoEntity::class,
    GlobalSettingEntity::class,
    LocationEntity::class,
    NoteEntity::class,
    PlanterDetails::class,
    PlanterIndentifications::class,
    SettingsEntity::class,
    //SqliteSequenceEntity::class,
    PhotoEntity::class
], version = 2)
abstract class AppDatabase : RoomDatabase() {

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                                                    AppDatabase::class.java,
                                                    "treetracker.v2.db")
                        .addMigrations(MigrationV1ToV2())
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}

class MigrationV1ToV2 : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        // WIP to convert Y/N boolean values to integers for Room to use
//        database.beginTransaction()
//
//        val isSyncedInts: List<Pair<Long, Int>> = database.loadBooleans(TreeEntity.TABLE, TreeEntity.IS_SYNCED)
//
//        database.endTransaction()
    }

    fun <T> SupportSQLiteDatabase.loadColumn(table: String, column: String, block: Cursor.(column: String) -> T): List<Pair<Long, T>> {
        val cursor = this.query("""
            SELECT _id, $column FROM $table;
        """.trimIndent())

        val columnValues = mutableListOf<Pair<Long, T>>()
        while(cursor.moveToNext()) {
            columnValues.add(cursor.loadLong("_id") to cursor.block(column))
        }
        return columnValues
    }

    fun SupportSQLiteDatabase.loadBooleans(table: String, column: String): List<Pair<Long, Int>> {
        return loadColumn(table, column) {
            if (loadString(it) == "Y") 1 else 0
        }
    }
}

fun Cursor.loadString(id: String): String? = getString(getColumnIndex(id))
fun Cursor.loadFloat(id: String): Float = getFloat(getColumnIndex(id))
fun Cursor.loadLong(id: String): Long = getLong(getColumnIndex(id))
fun Cursor.loadDouble(id: String): Double = getDouble(getColumnIndex(id))