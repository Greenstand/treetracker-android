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
import org.greenstand.android.TreeTracker.database.entity.LocationEntity

@Dao
interface LocationDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationData(locationEntity: LocationEntity): Long

    @Query("SELECT * FROM location WHERE uploaded = 0")
    suspend fun getLocationData(): List<LocationEntity>

    @Query(
        "SELECT l.* FROM location l " + "INNER JOIN session s ON l.session_id = s._id " + "INNER JOIN tree t ON t.session_id = s._id " + "GROUP BY l._id " + "ORDER BY l._id DESC " + "LIMIT :limit",
    )
    suspend fun getLocationsForTreeSessions(limit: Int = 5000): List<LocationEntity>

    @Query("UPDATE location_data SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateLegacyLocationDataUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )

    @Query("UPDATE location SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateLocationDataUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )

    @Query("DELETE FROM location_data WHERE uploaded = 1")
    suspend fun purgeUploadedTreeLocations()

    @Query("DELETE FROM location WHERE uploaded = 1")
    suspend fun purgeUploadedLocations()
}
