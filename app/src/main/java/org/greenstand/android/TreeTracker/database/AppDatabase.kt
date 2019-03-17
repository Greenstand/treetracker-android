package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.greenstand.android.TreeTracker.database.entities.*
import org.greenstand.android.TreeTracker.database.migrations.MigrationV1ToV2


@Database(
    entities = [
        TreeAttributesEntity::class,
        TreeEntity::class,
        TreeNoteEntity::class,
        TreePhotoEntity::class,
        LocationEntity::class,
        NoteEntity::class,
        PlanterDetailsEntity::class,
        PlanterIndentificationsEntity::class,
        SettingsEntity::class,
        PendingUpdateEntity::class,
        PhotoEntity::class
    ], version = 2
)
abstract class AppDatabase : RoomDatabase() {

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME_V2
                    ).addMigrations(MigrationV1ToV2())
                        .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME_V2 = "treetracker.v2.db"
    }
}

