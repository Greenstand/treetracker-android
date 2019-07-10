package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.utilities.ValueHelper


data class CreatePlanterCheckInParams(val localPhotoPath: String,
                                    val identifier: String)

class CreatePlanterCheckInUseCase(private val sharedPreferences: SharedPreferences,
                                  private val userLocationManager: UserLocationManager,
                                  private val doa: TreeTrackerDAO,
                                  private val analytics: Analytics
) : UseCase<CreatePlanterCheckInParams, Long>() {

    override suspend fun execute(params: CreatePlanterCheckInParams): Long = withContext(Dispatchers.IO) {

        val planterInfoId = doa.getPlanterInfoIdByIdentifier(params.identifier)

        val location = userLocationManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()

        val entity = PlanterCheckInEntity(
            planterInfoId = planterInfoId,
            identifier = params.identifier,
            localPhotoPath = params.localPhotoPath,
            longitude = location?.longitude ?: 0.0,
            latitude = location?.latitude ?: 0.0,
            createdAt = time,
            photoUrl = null
        )

        doa.insertPlanterCheckIn(entity).also {

            analytics.userCheckedIn()

            sharedPreferences.edit()
                .putString(ValueHelper.PLANTER_IDENTIFIER, params.identifier)
                .putLong(ValueHelper.PLANTER_IDENTIFIER_ID, planterInfoId)
                .putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, time / 1000)
                .putString(ValueHelper.PLANTER_PHOTO, params.localPhotoPath)
                .apply()
        }
    }
}