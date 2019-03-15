package org.greenstand.android.TreeTracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
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
    PlantIndentifications::class,
    SettingsEntity::class,
    SqliteSequenceEntity::class,
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

    }

}