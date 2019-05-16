package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.greenstand.android.TreeTracker.database.dao.*
import org.greenstand.android.TreeTracker.database.entity.*
import org.greenstand.android.TreeTracker.database.migration.MigrationV1ToV2
import org.greenstand.android.TreeTracker.database.migration.MigrationV2ToV3


@Database(
    entities = [
        TreeAttributesEntity::class,
        TreeEntity::class,
        TreeNoteEntity::class,
        TreePhotoEntity::class,
        LocationEntity::class,
        NoteEntity::class,
        PlanterDetailsEntity::class,
        PlanterIdentificationsEntity::class,
        SettingsEntity::class,
        PendingUpdateEntity::class,
        PhotoEntity::class
    ],
    version = 3,
    exportSchema = true
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun treeDao(): TreeDao
    abstract fun planterDao(): PlanterDao
    abstract fun photoDao(): PhotoDao
    abstract fun locationDao(): LocationDao
    abstract fun noteDao(): NoteDao
    abstract fun settingsDao(): SettingsDao
    abstract fun treeAttributesDao(): TreeAttributesDao

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                                                    AppDatabase::class.java,
                                                    DB_NAME_V2)
                        .addMigrations(MigrationV1ToV2())
                        .addMigrations(MigrationV2ToV3())
                        .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME_V2 = "treetracker.v2.db"
    }

}

