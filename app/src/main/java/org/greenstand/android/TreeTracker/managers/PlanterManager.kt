package org.greenstand.android.TreeTracker.managers

import android.content.Context
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

object PlanterManager {

    private val log = Timber.tag("PlanterManager")

    private val sharedPrefs by lazy {
        TreeTrackerApplication.appContext.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
    }

    private val db = TreeTrackerApplication.getAppDatabase()

    suspend fun addPlanterIdentification(identifier: String,
                                         photoPath: String): Long {

        log.d("Adding Planter Identification: $identifier")

        val planterDetailsId: Int? = getPlantersByIdentifier(identifier).firstOrNull()?.id

        log.d("Found planter details ID = $planterDetailsId")

        val entity = PlanterIdentificationsEntity(
                planterDetailsId?.toLong(),
                identifier,
                photoPath,
                null,
                System.currentTimeMillis().toString()
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
                                  timeCreated: String): Long {


        val planterDetailsEntity = PlanterDetailsEntity(
            identification,
            firstName,
            lastName,
            organization,
            null,
            null,
            false,
            timeCreated
        )

        return db.planterDao().insertPlanterDetails(planterDetailsEntity)
    }

    suspend fun getPlantersByIdentifier(identifier: String): List<PlanterDetailsEntity> {
        return db.planterDao().getPlantersByIdentifier(identifier)
    }

    fun getPlanterByInputtedText(identifier: String): PlanterDetailsEntity? {
        return db.planterDao().getPlanterDetailsByIdentifier(identifier)
    }

    suspend fun insertPlanter(identifier: String,
                              firstName: String,
                              lastName: String,
                              organization: String,
                              phone: String,
                              email: String,
                              uploaded: Boolean = false,
                              timeCreated: String): Long {

        val planterEntity = PlanterDetailsEntity(
            identifier = identifier,
            firstName = firstName,
            lastName = lastName,
            organization = organization,
            phone = phone,
            email = email,
            uploaded = uploaded,
            timeCreated = timeCreated
        )

        return db.planterDao().insertPlanterDetails(planterEntity)
            .also { Timber.d("PlanterDetails $it") }
    }

    suspend fun insertPlanterIdentification(planterDetailsId: Long,
                                            identifier: String,
                                            photoPath: String,
                                            photoUrl: String,
                                            timeCreated: String): Long {

        val planterIdentificationsEntity = PlanterIdentificationsEntity(
            planterDetailsId = planterDetailsId,
            identifier = identifier,
            photoPath =  photoPath,
            photoUrl = photoUrl,
            timeCreated = timeCreated
        )

        return db.planterDao().insertPlanterIdentifications(planterIdentificationsEntity)
            .also { Timber.d("PlanterDetails $it") }
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