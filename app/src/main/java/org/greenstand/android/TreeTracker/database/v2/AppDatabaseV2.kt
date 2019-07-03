package org.greenstand.android.TreeTracker.database.v2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.v2.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.v2.entity.TreeCaptureEntity

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

abstract class AppDatabaseV2 : RoomDatabase() {

    abstract fun treeTrackerDao(): TreeTrackerDAO


    companion object {

        private var INSTANCE: AppDatabaseV2? = null

        fun getInstance(context: Context): AppDatabaseV2 {
            if (INSTANCE == null) {
                synchronized(AppDatabaseV2::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                                                    AppDatabaseV2::class.java,
                                                    DB_NAME_V2)
                        .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME_V2 = "treetracker.v3.db"
    }

}

