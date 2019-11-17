package org.greenstand.android.TreeTracker.database

import androidx.room.*
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.database.views.TreeMapMarkerDbView


@Dao
interface TreeTrackerDAO {

    @Query("SELECT _id FROM planter_info WHERE planterInfoId = :identifier")
    fun getPlanterInfoIdByIdentifier(identifier: String): Long?

    @Query("SELECT * FROM planter_info")
    fun getAllPlanterInfo(): List<PlanterInfoEntity>

    @Query("SELECT * FROM planter_info WHERE _id = :id")
    fun getPlanterInfoById(id: Long): PlanterInfoEntity?

    @Update
    fun updatePlanterInfo(planterInfoEntity: PlanterInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlanterInfo(planterInfoEntity: PlanterInfoEntity): Long

    @Delete
    fun deletePlanterInfo(planterInfoEntity: PlanterInfoEntity)



    @Transaction
    @Query("SELECT * FROM planter_check_in WHERE photo_url IS null AND planter_info_id = :planterInfoId")
    fun getPlanterCheckInsToUpload(planterInfoId: Long): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in")
    fun getAllPlanterCheckIn(): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in WHERE _id = :id")
    fun getPlanterCheckInById(id: Long): PlanterCheckInEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity): Long

    @Update
    fun updatePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)

    @Delete
    fun deletePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)


    @Query("SELECT latitude, longitude, _id as treeCaptureId FROM tree_capture")
    fun getTreeDataForMap(): List<TreeMapMarkerDbView>

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 1")
    fun getUploadedTreeCaptureCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM tree_capture WHERE uploaded = 0")
    fun getNonUploadedTreeCaptureCount(): Int

    @Query("SELECT _id FROM tree_capture WHERE uploaded = 0")
    fun getAllTreeCaptureIdsToUpload(): List<Long>

    @Query("SELECT * FROM tree_capture")
    fun getAllTreeCaptures(): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree_capture WHERE _id = :id")
    fun getTreeCaptureById(id: Long): TreeCaptureEntity

    @Query("SELECT * FROM tree_capture WHERE _id IN (:ids)")
    fun getTreeCapturesByIds(ids: List<Long>): List<TreeCaptureEntity>

//    @Transaction
//    @Query("SELECT * FROM tree_capture WHERE _id = :id")
//    fun getTreeUploadDataById(id: Long): TreeUploadDbView

    @Query("UPDATE tree_capture SET bundle_id = :bundleId WHERE _id IN (:ids)")
    fun updateTreeCapturesBundleIds(ids: List<Long>, bundleId: String)

    @Query("UPDATE tree_capture SET uploaded = :isUploaded WHERE _id IN (:ids)")
    fun updateTreeCapturesUploadStatus(ids: List<Long>, isUploaded: Boolean)

    @Query("UPDATE tree_capture SET local_photo_path = null WHERE _id IN (:ids)")
    fun removeTreeCapturesLocalImagePaths(ids: List<Long>)

    @Update
    fun updateTreeCapture(treeCaptureEntity: TreeCaptureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTreeCapture(treeCaptureEntity: TreeCaptureEntity): Long

    @Delete
    fun deleteTreeCapture(treeCaptureEntity: TreeCaptureEntity)




    @Query("SELECT * FROM tree_attribute")
    fun getAllTreeAttributes(): List<TreeAttributeEntity>

    @Query("SELECT * FROM tree_attribute WHERE _id = :id")
    fun getTreeAttributeById(id: Long): TreeAttributeEntity

    @Query("SELECT * FROM tree_attribute WHERE tree_capture_id = :treeCaptureId")
    fun getTreeAttributeByTreeCaptureId(treeCaptureId: Long): List<TreeAttributeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTreeAttribute(treeAttributeEntity: TreeAttributeEntity): Long

    @Delete
    fun deleteTreeAttribute(treeAttributeEntity: TreeAttributeEntity)
}