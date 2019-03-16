package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.greenstand.android.TreeTracker.database.entities.*
import androidx.room.Room


@Database(entities = [
    TreeAttributesEntity::class,
    TreeEntity::class,
    TreeNoteEntity::class,
    TreePhotoEntity::class,
    GlobalSettingEntity::class,
    LocationEntity::class,
    NoteEntity::class,
    PlanterDetailsEntity::class,
    PlanterIndentificationsEntity::class,
    SettingsEntity::class,
    PendingUpdateEntity::class,
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
