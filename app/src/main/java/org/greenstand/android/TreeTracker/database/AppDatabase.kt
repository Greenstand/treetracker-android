package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity

@Database(
    version = 9,
    exportSchema = true,
    entities = [
        PlanterCheckInEntity::class,
        PlanterInfoEntity::class,
        TreeAttributeEntity::class,
        TreeCaptureEntity::class,
        LocationDataEntity::class,
        SessionEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 8, to = 9)
    ],
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treeTrackerDao(): TreeTrackerDAO

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        .addMigrations(
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                        )
                        .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME = "treetracker.v2.db"
    }
}
