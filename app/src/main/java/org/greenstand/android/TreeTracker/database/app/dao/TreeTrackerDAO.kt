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
package org.greenstand.android.TreeTracker.database.app.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.greenstand.android.TreeTracker.database.app.entity.DeviceConfigEntity
import org.greenstand.android.TreeTracker.database.app.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.app.entity.OrganizationEntity
import org.greenstand.android.TreeTracker.database.app.entity.SessionEntity
import org.greenstand.android.TreeTracker.database.app.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.app.entity.UserEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.app.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.database.app.legacy.views.TreeMapMarkerDbView

@Dao
interface TreeTrackerDAO {

    @Query("SELECT _id FROM planter_info WHERE planter_identifier = :identifier")
    suspend fun getPlanterInfoIdByIdentifier(identifier: String): Long?

    @Query("SELECT * FROM planter_info")
    fun getAllPlanterInfo(): Flow<List<PlanterInfoEntity>>

    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM planter_info")
    suspend fun getAllPlanterInfoList(): List<PlanterInfoEntity>

    @Query("SELECT * FROM user")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM planter_info where uploaded = 0")
    suspend fun getAllPlanterInfoToUpload(): List<PlanterInfoEntity>

    @Query("SELECT * FROM user where uploaded = 0")
    suspend fun getAllUsersToUpload(): List<UserEntity>

    @Query("SELECT * FROM planter_info WHERE _id = :id")
    suspend fun getPlanterInfoById(id: Long): PlanterInfoEntity?

    @Query("SELECT * FROM user WHERE _id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM planter_info WHERE planter_identifier = :identity")
    suspend fun getPlanterInfoByIdentifier(identity: String): PlanterInfoEntity?

    @Query("SELECT * FROM user WHERE wallet = :wallet")
    suspend fun getUserByWallet(wallet: String): UserEntity?

    @Query("SELECT * FROM user WHERE power_user = 1")
    suspend fun getPowerUser(): UserEntity?

    @Update
    suspend fun updatePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Update
    suspend fun updateUser(userEntity: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanterInfo(planterInfoEntity: PlanterInfoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserEntity): Long

    @Delete
    suspend fun deletePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Query("UPDATE planter_info SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updatePlanterInfoBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE user SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateUserBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE planter_info SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updatePlanterInfoUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("UPDATE user SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateUserUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Transaction
    @Query("SELECT * FROM planter_check_in WHERE local_photo_path IS NOT NULL")
    suspend fun getPlanterCheckInsToUpload(): List<PlanterCheckInEntity>

    @Transaction
    @Query("SELECT * FROM planter_check_in WHERE _id IN (:planterCheckInIds)")
    suspend fun getPlanterCheckInsById(planterCheckInIds: List<Long>): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in WHERE planter_info_id = (:planterInfoId)")
    suspend fun getAllPlanterCheckInsForPlanterInfoId(planterInfoId: Long): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in WHERE _id = :id")
    suspend fun getPlanterCheckInById(id: Long): PlanterCheckInEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity): Long

    @Update
    suspend fun updatePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)

    @Delete
    suspend fun deletePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)

    @Query("UPDATE planter_check_in SET local_photo_path = null WHERE _id IN (:ids)")
    suspend fun removePlanterCheckInLocalImagePaths(ids: List<Long>)

    @Query("SELECT latitude, longitude, _id as treeCaptureId FROM tree_capture")
    suspend fun getTreeDataForMap(): List<TreeMapMarkerDbView>

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 1")
    suspend fun getUploadedTreeCaptureCount(): Int

    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url not null")
    suspend fun getUploadedLegacyTreeImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree WHERE photo_url not null")
    suspend fun getUploadedTreeImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree WHERE uploaded = 1")
    suspend fun getUploadedTreeCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url is null")
    suspend fun getNonUploadedLegacyTreeCaptureImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree WHERE photo_url is null")
    suspend fun getNonUploadedTreeImageCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 0")
    suspend fun getNonUploadedTreeCaptureCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree WHERE uploaded = 0")
    suspend fun getNonUploadedTreeCount(): Int

    @Query("SELECT _id FROM tree_capture WHERE uploaded = 0")
    suspend fun getAllTreeCaptureIdsToUpload(): List<Long>

    @Query("SELECT _id FROM tree WHERE uploaded = 0")
    suspend fun getAllTreeIdsToUpload(): List<Long>

    @Query("SELECT COUNT(*) FROM tree WHERE session_id = :sessionId")
    suspend fun getTreeCountFromSessionId(sessionId: Long): Int

    @Query("SELECT * FROM tree_capture")
    suspend fun getAllTreeCaptures(): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree_capture WHERE _id = :id")
    suspend fun getTreeCaptureById(id: Long): TreeCaptureEntity

    @Query("SELECT * FROM tree_capture WHERE _id IN (:ids)")
    suspend fun getTreeCapturesByIds(ids: List<Long>): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree WHERE _id IN (:ids)")
    suspend fun getTreesByIds(ids: List<Long>): List<TreeEntity>

    @Query("UPDATE tree_capture SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateTreeCapturesBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE tree SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateTreesBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE tree SET uploaded = :uploaded WHERE _id IN (:ids)")
    suspend fun updateTreesUploadStatus(ids: List<Long>, uploaded: Boolean)

    @Query("UPDATE tree_capture SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateTreeCapturesUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("UPDATE tree_capture SET local_photo_path = null WHERE _id IN (:ids)")
    suspend fun removeTreeCapturesLocalImagePaths(ids: List<Long>)

    @Query("UPDATE tree SET photo_path = null WHERE _id IN (:ids)")
    suspend fun removeTreesLocalImagePaths(ids: List<Long>)

    @Update
    suspend fun updateTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Update
    suspend fun updateTree(treeEntity: TreeEntity)

    @Delete
    suspend fun deleteTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Query("SELECT * FROM tree_attribute WHERE tree_capture_id = :treeCaptureId")
    suspend fun getTreeAttributeByTreeCaptureId(treeCaptureId: Long): List<TreeAttributeEntity>

    @Insert
    suspend fun insertTree(treeEntity: TreeEntity): Long

    @Transaction
    suspend fun insertTreeWithAttributes(
        tree: TreeCaptureEntity,
        attributes: List<TreeAttributeEntity>?
    ): Long {
        val treeId = insertTreeCapture(tree)
        attributes?.forEach {
            it.treeCaptureId = treeId
            insertTreeAttribute(it)
        }
        return treeId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeCapture(treeCaptureEntity: TreeCaptureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeAttribute(treeAttributeEntity: TreeAttributeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationData(locationEntity: LocationEntity): Long

    @Query("SELECT * FROM location WHERE uploaded = 0")
    suspend fun getLocationData(): List<LocationEntity>

    @Query("UPDATE location_data SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateLegacyLocationDataUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("UPDATE location SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateLocationDataUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("DELETE FROM location_data WHERE uploaded = 1")
    suspend fun purgeUploadedTreeLocations()

    @Query("DELETE FROM location WHERE uploaded = 1")
    suspend fun purgeUploadedLocations()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(sessionEntity: SessionEntity): Long

    @Update
    suspend fun updateSession(sessionEntity: SessionEntity)

    @Query("SELECT * FROM session WHERE _id = :id")
    suspend fun getSessionById(id: Long): SessionEntity

    @Query("SELECT * FROM session WHERE origin_wallet = :wallet")
    suspend fun getSessionsByUserWallet(wallet: String): List<SessionEntity>

    @Query("SELECT * FROM session WHERE uploaded = 0")
    suspend fun getSessionsToUpload(): List<SessionEntity>

    @Query("UPDATE session SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateSessionBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE session SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateSessionUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("SELECT * FROM device_config WHERE uploaded = 0")
    suspend fun getDeviceConfigsToUpload(): List<DeviceConfigEntity>

    @Query("SELECT * FROM device_config WHERE _id = (:id)")
    suspend fun getDeviceConfigById(id: Long): DeviceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceConfig(deviceConfig: DeviceConfigEntity): Long

    @Query("UPDATE device_config SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateDeviceConfigBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE device_config SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateDeviceConfigUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("SELECT * FROM device_config ORDER BY logged_at DESC LIMIT 1")
    suspend fun getLatestDeviceConfig(): DeviceConfigEntity?

    /**
     * ORG LINK QUERIES
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrg(orgEntity: OrganizationEntity): Long

    @Query("SELECT * FROM organization WHERE _id = :id")
    suspend fun getOrg(id: String?): OrganizationEntity?

    @Query("SELECT * FROM organization")
    suspend fun getAllOrgs(): List<OrganizationEntity>
}