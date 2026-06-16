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
package org.greenstand.android.TreeTracker.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity

@Dao
interface DeviceConfigDAO {
    @Query("SELECT * FROM device_config WHERE uploaded = 0")
    suspend fun getDeviceConfigsToUpload(): List<DeviceConfigEntity>

    @Query("SELECT * FROM device_config WHERE _id = (:id)")
    suspend fun getDeviceConfigById(id: Long): DeviceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceConfig(deviceConfig: DeviceConfigEntity): Long

    @Query("UPDATE device_config SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateDeviceConfigBundleIds(
        ids: List<Long>,
        bundleId: String,
    )

    @Query("UPDATE device_config SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateDeviceConfigUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )

    @Query("SELECT * FROM device_config ORDER BY logged_at DESC LIMIT 1")
    suspend fun getLatestDeviceConfig(): DeviceConfigEntity?
}
