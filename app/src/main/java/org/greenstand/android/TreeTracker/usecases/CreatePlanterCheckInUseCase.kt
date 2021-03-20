package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.User

data class CreatePlanterCheckInParams(
    val localPhotoPath: String,
    val planterInfoId: Long
)

@Deprecated("Use Users class instead to start a session, which replaces check ins")
class CreatePlanterCheckInUseCase(
    private val locationUpdateManager: LocationUpdateManager,
    private val doa: TreeTrackerDAO,
    private val analytics: Analytics,
    private val user: User
) : UseCase<CreatePlanterCheckInParams, Long>() {

    override suspend fun execute(params: CreatePlanterCheckInParams): Long =
        withContext(Dispatchers.IO) {

            val location = locationUpdateManager.currentLocation
            val time = location?.time ?: System.currentTimeMillis()

            val entity = PlanterCheckInEntity(
                planterInfoId = params.planterInfoId,
                localPhotoPath = params.localPhotoPath,
                longitude = location?.longitude ?: 0.0,
                latitude = location?.latitude ?: 0.0,
                createdAt = time,
                photoUrl = null
            )

            val planterCheckInId = doa.insertPlanterCheckIn(entity)

            doa.getPlanterInfoById(params.planterInfoId)?.let { planterInfo ->
                with(user) {
                    firstName = planterInfo.firstName
                    lastName = planterInfo.lastName
                    organization = planterInfo.organization
                    planterCheckinId = planterCheckInId
                    planterInfoId = params.planterInfoId
                    lastCheckInTimeInSeconds = System.currentTimeMillis() / 1000
                    profilePhotoPath = params.localPhotoPath
                }
            }

            analytics.userCheckedIn()

            planterCheckInId
        }
}
