package org.greenstand.android.TreeTracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.greenstand.android.TreeTracker.database.entity.LocationDataEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.database.views.TreeMapMarkerDbView

@Dao
interface TreeTrackerDAO {

    @Query("SELECT _id FROM planter_info WHERE planter_identifier = :identifier")
    suspend fun getPlanterInfoIdByIdentifier(identifier: String): Long?

    @Query("SELECT * FROM planter_info")
    suspend fun getAllPlanterInfo(): Flow<List<PlanterInfoEntity>>

    @Query("SELECT * FROM planter_info where uploaded = 0")
    suspend fun getAllPlanterInfoToUpload(): List<PlanterInfoEntity>

    @Query("SELECT * FROM planter_info WHERE _id = :id")
    suspend fun getPlanterInfoById(id: Long): PlanterInfoEntity?

    @Query("SELECT * FROM planter_info WHERE power_user = 1")
    suspend fun getPowerUser(): PlanterInfoEntity?

    @Update
    suspend fun updatePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanterInfo(planterInfoEntity: PlanterInfoEntity): Long

    @Delete
    suspend fun deletePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Query("UPDATE planter_info SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updatePlanterInfoBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE planter_info SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updatePlanterInfoUploadStatus(ids: List<Long>, isUploaded: Boolean)

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
    suspend fun getUploadedTreeImageCount(): Int

    @Query("SELECT COUNT(*) FROM tree_capture WHERE photo_url is null")
    suspend fun getNonUploadedTreeImageCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 0")
    suspend fun getNonUploadedTreeCaptureCount(): Int

    @Query("SELECT _id FROM tree_capture WHERE uploaded = 0")
    suspend fun getAllTreeCaptureIdsToUpload(): List<Long>

    @Query("SELECT * FROM tree_capture")
    suspend fun getAllTreeCaptures(): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree_capture WHERE _id = :id")
    suspend fun getTreeCaptureById(id: Long): TreeCaptureEntity

    @Query("SELECT * FROM tree_capture WHERE _id IN (:ids)")
    suspend fun getTreeCapturesByIds(ids: List<Long>): List<TreeCaptureEntity>

    @Query("UPDATE tree_capture SET bundle_id = :bundleId WHERE _id IN (:ids)")
    suspend fun updateTreeCapturesBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE tree_capture SET uploaded = :isUploaded WHERE _id IN (:ids)")
    suspend fun updateTreeCapturesUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("UPDATE tree_capture SET local_photo_path = null WHERE _id IN (:ids)")
    suspend fun removeTreeCapturesLocalImagePaths(ids: List<Long>)

    @Update
    suspend fun updateTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeCapture(treeCaptureEntity: TreeCaptureEntity): Long

    @Delete
    suspend fun deleteTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Query("SELECT * FROM tree_attribute WHERE _id = :id")
    suspend fun getTreeAttributeById(id: Long): TreeAttributeEntity

    @Query("SELECT * FROM tree_attribute WHERE tree_capture_id = :treeCaptureId")
    suspend fun getTreeAttributeByTreeCaptureId(treeCaptureId: Long): List<TreeAttributeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeAttribute(treeAttributeEntity: TreeAttributeEntity): Long

    @Delete
    suspend fun deleteTreeAttribute(treeAttributeEntity: TreeAttributeEntity)

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
    suspend fun insertLocationData(locationDataEntity: LocationDataEntity): Long

    @Query("SELECT * FROM location_data WHERE uploaded = 0")
    suspend fun getTreeLocationData(): List<LocationDataEntity>

    @Update
    suspend fun updateLocationData(locationDataEntity: LocationDataEntity)

    @Query("DELETE FROM location_data WHERE uploaded = 1")
    suspend fun purgeUploadedTreeLocations()
}
