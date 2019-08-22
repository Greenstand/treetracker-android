package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.managers.UserLocationManager


data class CreatePlanterInfoParams(val firstName: String,
                                   val lastName: String,
                                   val organization: String?,
                                   val phone: String?,
                                   val email: String?,
                                   val identifier: String)

class CreatePlanterInfoUseCase(private val userLocationManager: UserLocationManager,
                               private val doa: TreeTrackerDAO,
                               private val analytics: Analytics) : UseCase<CreatePlanterInfoParams, Long>() {

    override suspend fun execute(params: CreatePlanterInfoParams): Long = withContext(Dispatchers.IO) {

        val location = userLocationManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()

        val entity = PlanterInfoEntity(
            identifier = params.identifier,
            firstName = params.firstName,
            lastName = params.lastName,
            organization = params.organization,
            phone = params.phone,
            email = params.email,
            longitude = location?.longitude ?: 0.0,
            latitude = location?.latitude ?: 0.0,
            createdAt = time
        )

        doa.insertPlanterInfo(entity).also {
            analytics.userInfoCreated(phone = params.phone.orEmpty(),
                                      email = params.email.orEmpty())
        }
    }

}