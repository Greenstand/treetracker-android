package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.ValueHelper


data class CreatePlanterCheckInParams(val localPhotoPath: String,
                                      val planterInfoId: Long)

class CreatePlanterCheckInUseCase(private val sharedPreferences: SharedPreferences,
                                  private val userLocationManager: UserLocationManager,
                                  private val doa: TreeTrackerDAO,
                                  private val analytics: Analytics,
                                  private val userManager: UserManager) : UseCase<CreatePlanterCheckInParams, Long>() {

    override suspend fun execute(params: CreatePlanterCheckInParams): Long = withContext(Dispatchers.IO) {

        val location = userLocationManager.currentLocation
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

        analytics.userCheckedIn()

        sharedPreferences.edit()
            .putLong(ValueHelper.PLANTER_CHECK_IN_ID, planterCheckInId)
            .putLong(ValueHelper.PLANTER_INFO_ID, params.planterInfoId)
            .putLong(ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS, System.currentTimeMillis() / 1000)
            .putString(ValueHelper.PLANTER_PHOTO, params.localPhotoPath)
            .apply()

        doa.getPlanterInfoById(params.planterInfoId)?.let {
            userManager.firstName = it.firstName
            userManager.lastName = it.lastName
            userManager.organization = it.organization
        }

        planterCheckInId
    }
}