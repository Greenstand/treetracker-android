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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity

@Dao
interface PlanterDAO {
    @Query("SELECT _id FROM planter_info WHERE planter_identifier = :identifier")
    suspend fun getPlanterInfoIdByIdentifier(identifier: String): Long?

    @Query("SELECT * FROM planter_info")
    fun getAllPlanterInfo(): Flow<List<PlanterInfoEntity>>

    @Query("SELECT * FROM planter_info")
    suspend fun getAllPlanterInfoList(): List<PlanterInfoEntity>

    @Query("SELECT * FROM planter_info where uploaded = 0")
    suspend fun getAllPlanterInfoToUpload(): List<PlanterInfoEntity>

    @Query("SELECT * FROM planter_info WHERE _id = :id")
    suspend fun getPlanterInfoById(id: Long): PlanterInfoEntity?

    @Query("SELECT * FROM planter_info WHERE planter_identifier = :identity")
    suspend fun getPlanterInfoByIdentifier(identity: String): PlanterInfoEntity?

    @Update
    suspend fun updatePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanterInfo(planterInfoEntity: PlanterInfoEntity): Long

    @Delete
    suspend fun deletePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Query("UPDATE planter_info SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updatePlanterInfoBundleIds(
        ids: List<Long>,
        bundleId: String,
    )

    @Query("UPDATE planter_info SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updatePlanterInfoUploadStatus(
        ids: List<Long>,
        isUploaded: Boolean,
    )

    @Transaction
    @Query("SELECT * FROM planter_check_in WHERE local_photo_path IS NOT NULL")
    suspend fun getPlanterCheckInsToUpload(): List<PlanterCheckInEntity>

    @Transaction
    @Query("SELECT * FROM planter_check_in WHERE _id IN (:planterCheckInIds)")
    suspend fun getPlanterCheckInsById(planterCheckInIds: List<Long>): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in WHERE planter_info_id = (:planterInfoId)")
    suspend fun getAllPlanterCheckInsForPlanterInfoId(planterInfoId: Long): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in WHERE _id = :id")
    suspend fun getPlanterCheckInById(id: Long): PlanterCheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity): Long

    @Update
    suspend fun updatePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)

    @Delete
    suspend fun deletePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)

    @Query("UPDATE planter_check_in SET local_photo_path = null WHERE _id IN (:ids)")
    suspend fun removePlanterCheckInLocalImagePaths(ids: List<Long>)
}