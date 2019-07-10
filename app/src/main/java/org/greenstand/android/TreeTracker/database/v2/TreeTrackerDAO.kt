package org.greenstand.android.TreeTracker.database.v2

import androidx.room.*
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.v2.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.v2.entity.TreeCaptureEntity


@Dao
interface TreeTrackerDAO {

    @Transaction
    @Query("SELECT _id FROM planter_info WHERE identifier = :identifier")
    fun getPlanterInfoIdByIdentifier(identifier: String): Long


    @Query("SELECT * FROM planter_info")
    fun getAllPlanterInfo(): List<PlanterInfoEntity>

    @Query("SELECT * FROM planter_info WHERE _id = :id")
    fun getPlanterInfoById(id: Long): PlanterInfoEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlanterInfo(planterInfoEntity: PlanterInfoEntity): Long

    @Delete
    fun deletePlanterInfo(planterInfoEntity: PlanterInfoEntity)




    @Query("SELECT * FROM planter_check_in")
    fun getAllPlanterCheckIn(): List<PlanterCheckInEntity>

    @Query("SELECT * FROM planter_check_in WHERE _id = :id")
    fun getPlanterCheckInById(id: Long): PlanterCheckInEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity): Long

    @Delete
    fun deletePlanterCheckIn(planterCheckInEntity: PlanterCheckInEntity)




    @Query("SELECT * FROM tree_capture")
    fun getAllTreeCaptures(): List<TreeCaptureEntity>

    @Query("SELECT * FROM tree_capture WHERE _id = :id")
    fun getTreeCaptureById(id: Long): TreeCaptureEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTreeCapture(treeCaptureEntity: TreeCaptureEntity): Long

    @Delete
    fun deleteTreeCapture(treeCaptureEntity: TreeCaptureEntity)




    @Query("SELECT * FROM tree_attribute")
    fun getAllTreeAttributes(): List<TreeAttributeEntity>

    @Query("SELECT * FROM tree_attribute WHERE _id = :id")
    fun getTreeAttributeById(id: Long): TreeAttributeEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTreeAttribute(treeAttributeEntity: TreeAttributeEntity): Long

    @Delete
    fun deleteTreeAttribute(treeAttributeEntity: TreeAttributeEntity)
}