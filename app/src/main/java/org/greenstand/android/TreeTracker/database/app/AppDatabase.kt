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
package org.greenstand.android.TreeTracker.database.app

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.greenstand.android.TreeTracker.database.app.dao.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.app.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.database.app.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.app.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.database.app.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.app.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.app.entity.UserEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.TreeCaptureEntity

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
        // 8 -> 9 for v2.2
        AutoMigration(from = 8, to = 9)
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treeTrackerDao(): TreeTrackerDAO

    companion object {

        const val DB_NAME = "treetracker.v2.db"
        const val DB_NAME_ENCRYPT = "treetracker.v2-encrypt.db"

    }
}