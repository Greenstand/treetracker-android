package org.greenstand.android.TreeTracker.database.v2

import androidx.room.*
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterInfoId


@Dao
interface TreeTrackerDAO {

    @Query("SELECT * FROM planter_info")
    suspend fun getAllPlanterInfo(): List<PlanterInfoEntity>

    @Query("SELECT * FROM planter_info WHERE _id = :id")
    suspend fun getPlanterInfoById(id: PlanterInfoId): PlanterInfoEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanterInfo(planterInfoEntity: PlanterInfoEntity): PlanterInfoId

    @Delete
    suspend fun deletePlanterInfo(planterInfoEntity: PlanterInfoEntity)
}