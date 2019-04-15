package org.greenstand.android.TreeTracker.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity

@Dao
interface PlanterDao {
    @Transaction
    @Query("SELECT * FROM planter_details WHERE uploaded = 0")
    fun getPlanterRegistrationsToUpload(): List<PlanterDetailsEntity>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updatePlanterDetails(planterDetailsEntity: PlanterDetailsEntity)

    @Transaction
    @Query("SELECT * FROM planter_identifications WHERE identifier = :identifier")
    fun getPlanterIdentificationsByID(identifier: String?): List<PlanterIdentificationsEntity>

    @Transaction
    @Query("SELECT * FROM planter_identifications WHERE photo_url IS NULL")
    fun getPlanterIdentificationsToUpload(): List<PlanterIdentificationsEntity>

    @Transaction
    @Query("SELECT * FROM planter_details WHERE identifier = :identifier")
    fun getPlanterDetailsByIdentifier(identifier: String): LiveData<PlanterDetailsEntity>
    @Transaction
    @Query("SELECT * FROM planter_details WHERE identifier = :identifier")
    fun getPlanterDetailsIDByIdentifier(identifier: String): Int

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updatePlanterIdentification(planterIdentificationsEntity: PlanterIdentificationsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(planterIdentificationsEntity: PlanterIdentificationsEntity): Long

    @Transaction
    @Query("SELECT * FROM planter_details WHERE identifier = :identifier")
    fun getPlantersByIdentifier(identifier: String?): List<PlanterDetailsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(planterDetailsEntity: PlanterDetailsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlanterIdentifications(planterIdentificationsEntity: PlanterIdentificationsEntity): Long
}