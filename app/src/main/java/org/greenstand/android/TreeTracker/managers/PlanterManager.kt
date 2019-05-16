package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.location.Location
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

class PlanterManager(private val db: AppDatabase,
                     private val context: Context) {

    private val log = Timber.tag("PlanterManager")

    private val sharedPrefs by lazy {
        context.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
    }

    suspend fun addPlanterIdentification(identifier: String,
                                         photoPath: String,
                                         location: Location?): Long {

        log.d("Adding Planter Identification: $identifier")

        val planterDetailsId: Int? = getPlantersByIdentifier(identifier).firstOrNull()?.id

        log.d("Found planter details ID = $planterDetailsId")

        val entity = PlanterIdentificationsEntity(
                planterDetailsId?.toLong(),
                identifier,
                photoPath,
                null,
                System.currentTimeMillis().toString(),
                location = location?.let { "${it.latitude},${it.longitude}" }
        )

        log.d("Adding PlanterIdentification to DB:")
        log.d(entity.toString())

        val planterIndentifiedId = db.planterDao().insertPlanterIdentifications(entity)

        sharedPrefs.edit()
            .putString(ValueHelper.PLANTER_IDENTIFIER, identifier)
            .putLong(ValueHelper.PLANTER_IDENTIFIER_ID, planterIndentifiedId)
            .putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, System.currentTimeMillis() / 1000)
            .putString(ValueHelper.PLANTER_PHOTO, photoPath)
            .apply()

        return planterIndentifiedId
    }

    suspend fun addPlanterDetails(identification: String,
                                  firstName: String,
                                  lastName: String,
                                  organization: String?,
                                  timeCreated: String,
                                  location: Location?): Long {


        val planterDetailsEntity = PlanterDetailsEntity(
            identification,
            firstName,
            lastName,
            organization,
            null,
            null,
            false,
            timeCreated,
            location?.let { "${it.latitude},${it.longitude}" }
        )

        return db.planterDao().insertPlanterDetails(planterDetailsEntity)
    }

    suspend fun getPlantersByIdentifier(identifier: String): List<PlanterDetailsEntity> {
        return db.planterDao().getPlantersByIdentifier(identifier)
    }

    fun getPlanterByInputtedText(identifier: String): PlanterDetailsEntity? {
        return db.planterDao().getPlanterDetailsByIdentifier(identifier)
    }

    suspend fun updateIdentifierId(identifier: String, planterDetailsId: Long) {
        log.d("Updating Planter Identifier: $identifier with planterDetailsId: $planterDetailsId")

        val planterIdentification: PlanterIdentificationsEntity = db.planterDao().getPlanterIdentificationsByID(identifier).first()

        log.d("Loaded PlanterIdentification: $planterIdentification")

        planterIdentification.planterDetailsId = planterDetailsId
        db.planterDao().updatePlanterIdentification(planterIdentification)

        sharedPrefs.edit()
            .putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, System.currentTimeMillis() / 1000)
            .apply()
    }
}