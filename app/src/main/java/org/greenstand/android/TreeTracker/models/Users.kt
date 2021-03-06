package org.greenstand.android.TreeTracker.models

import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import timber.log.Timber

data class SessionUser(
    val planterInfo: PlanterInfoEntity,
    val planterCheckIn: PlanterCheckInEntity
)

class Users(
    private val locationUpdateManager: LocationUpdateManager,
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics
) {

    var currentSessionUser: SessionUser? = null
        private set

    suspend fun getUsers(): List<PlanterInfoEntity> = dao.getAllPlanterInfo()

    suspend fun createUser(
        firstName: String,
        lastName: String,
        organization: String?,
        phone: String?,
        email: String?,
        identifier: String
    ) {
        withContext(Dispatchers.IO) {

            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val entity = PlanterInfoEntity(
                identifier = identifier,
                firstName = firstName,
                lastName = lastName,
                organization = organization,
                phone = phone,
                email = email,
                longitude = location?.longitude ?: 0.0,
                latitude = location?.latitude ?: 0.0,
                createdAt = time,
                uploaded = false,
                recordUuid = UUID.randomUUID().toString()
            )

            dao.insertPlanterInfo(entity).also {
                analytics.userInfoCreated(
                    phone = phone.orEmpty(),
                    email = email.orEmpty()
                )
            }
        }

    }

    suspend fun startUserSession(
        localPhotoPath: String,
        planterInfoId: Long
    ) {
        withContext(Dispatchers.IO) {

            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val planterCheckInEntity = PlanterCheckInEntity(
                planterInfoId = planterInfoId,
                localPhotoPath = localPhotoPath,
                longitude = location?.longitude ?: 0.0,
                latitude = location?.latitude ?: 0.0,
                createdAt = time,
                photoUrl = null
            )

            dao.getPlanterInfoById(planterInfoId)?.let { planterInfo ->
                currentSessionUser = SessionUser(
                    planterCheckIn = planterCheckInEntity,
                    planterInfo = planterInfo
                )
            } ?: Timber.e("Could not find planter info of id $planterInfoId")

            analytics.userCheckedIn()
        }
    }

    fun endUserSession() {
        currentSessionUser = null
    }

}