/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.greenstand.android.TreeTracker.database.dao.DeviceConfigDAO
import org.greenstand.android.TreeTracker.database.dao.LocationDAO
import org.greenstand.android.TreeTracker.database.dao.OrganizationDAO
import org.greenstand.android.TreeTracker.database.dao.PlanterDAO
import org.greenstand.android.TreeTracker.database.dao.SessionDAO
import org.greenstand.android.TreeTracker.database.dao.TreeDAO
import org.greenstand.android.TreeTracker.database.dao.UserDAO
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity

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
        UserEntity::class,
        LocationEntity::class,
        TreeEntity::class,
        DeviceConfigEntity::class,
        OrganizationEntity::class,
    ],
    autoMigrations = [
        // 7 -> 8: adds the v2 tables (shipped in 2.0.0). The 1.x releases shipped schemas 3, 4 and 6
        // (1.4.0 = 6), and all of them upgrade through 7 -> 8, so this must stay.
        AutoMigration(from = 7, to = 8),
        // 8 -> 9: organization table and session.note (first shipped in 2.2.0)
        AutoMigration(from = 8, to = 9),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun treeDao(): TreeDAO

    abstract fun userDao(): UserDAO

    abstract fun sessionDao(): SessionDAO

    abstract fun organizationDao(): OrganizationDAO

    abstract fun deviceConfigDao(): DeviceConfigDAO

    abstract fun planterDao(): PlanterDAO

    abstract fun locationDao(): LocationDAO

    abstract fun treeTrackerDao(): TreeTrackerDAO

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = builder(context.applicationContext, DB_NAME).build()
                }
            }
            return INSTANCE!!
        }

        /**
         * A builder with the app's database configuration, for a database file named [name].
         * [getInstance] uses it, and tests use it to open a database exactly as the app does.
         */
        internal fun builder(
            context: Context,
            name: String,
        ): RoomDatabase.Builder<AppDatabase> =
            Room
                .databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(*MANUAL_MIGRATIONS)

        /** Hand-written migrations. Later versions use the auto migrations declared on @Database. */
        private val MANUAL_MIGRATIONS =
            arrayOf(
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
            )

        private const val DB_NAME = "treetracker.v2.db"
    }
}