package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.v2.entity.PlanterCheckInEntity

data class UploadPlanterParams(val planterInfoId: Long)

class UploadPlanterUseCase(private val dao: TreeTrackerDAO,
                           private val api: RetrofitApi,
                           private val uploadImageUseCase: UploadImageUseCase) : UseCase<UploadPlanterParams, Unit>() {

    override suspend fun execute(params: UploadPlanterParams) = withContext(Dispatchers.IO) {

        val planterInfoEntity = dao.getPlanterInfoById(params.planterInfoId)

        val registration = RegistrationRequest(
            planterIdentifier = planterInfoEntity.identifier,
            firstName = planterInfoEntity.firstName,
            lastName = planterInfoEntity.lastName,
            organization = planterInfoEntity.organization,
            location = "${planterInfoEntity.latitude},${planterInfoEntity.longitude}"
        )

        api.createPlanterRegistration(registration)

        planterInfoEntity.uploaded = true
        dao.updatePlanterInfo(planterInfoEntity)

        val planterCheckInListToUpload: List<PlanterCheckInEntity> = dao.getPlanterCheckInsToUpload(planterInfoEntity.id)

        planterCheckInListToUpload.forEach { planterCheckIn ->
            val imageUrl = uploadImageUseCase.execute(UploadImageParams(planterCheckIn.localPhotoPath))
            planterCheckIn.photoUrl = imageUrl
            dao.updatePlanterCheckIn(planterCheckIn)
        }
    }

}