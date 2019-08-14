package org.greenstand.android.TreeTracker.database.v2

import androidx.room.*
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.v2.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.v2.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.database.v2.views.TreeMapMarkerDbView


@Dao
interface TreeTrackerDAO {

    @Transaction
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

//    @Transaction
//    @Query("SELECT * FROM tree_capture WHERE _id = :id")
//    fun getTreeUploadDataById(id: Long): TreeUploadDbView

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