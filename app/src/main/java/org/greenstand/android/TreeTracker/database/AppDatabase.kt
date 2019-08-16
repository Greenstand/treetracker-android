package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity

@Database(
    entities = [
        PlanterCheckInEntity::class,
        PlanterInfoEntity::class,
        TreeAttributeEntity::class,
        TreeCaptureEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treeTrackerDao(): TreeTrackerDAO

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                                                    AppDatabase::class.java,
                                                    DB_NAME
                    )
                        .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME = "treetracker.v1.db"
    }

}

