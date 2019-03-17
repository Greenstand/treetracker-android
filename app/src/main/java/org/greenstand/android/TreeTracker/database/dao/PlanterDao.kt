package org.greenstand.android.TreeTracker.database.dao

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
    @Query("SELECT * FROM planter_identifications WHERE photo_url IS NULL")
    fun getPlanterIdentificationsToUpload(): List<PlanterIdentificationsEntity>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updatePlanterIdentification(it: PlanterIdentificationsEntity)

    @Transaction
    @Query("SELECT * FROM planter_details WHERE identifier = :identifier")
    fun getPlantersByIdentifier(identifier: String?): List<PlanterDetailsEntity>
}