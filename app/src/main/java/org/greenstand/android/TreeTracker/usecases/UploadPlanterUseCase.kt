package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.PlanterCheckInEntity
import timber.log.Timber

data class UploadPlanterParams(val planterInfoId: Long)


/**
 * Given a planter info ID this will upload the planters info
 * as well as each of the planters check in photos
 */
class UploadPlanterUseCase(private val dao: TreeTrackerDAO,
                           private val api: RetrofitApi,
                           private val uploadImageUseCase: UploadImageUseCase) : UseCase<UploadPlanterParams, Unit>() {

    private fun log(msg: String) = Timber.tag("UploadPlanterUseCase").d(msg)

    override suspend fun execute(params: UploadPlanterParams) = withContext(Dispatchers.IO) {

        log("Uploading planter ID: ${params.planterInfoId}")

        val planterInfoEntity = dao.getPlanterInfoById(params.planterInfoId)

        planterInfoEntity ?: throw IllegalStateException("No PlanterInfo for id = ${params.planterInfoId}")

        if (!planterInfoEntity.uploaded) {
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
        }

        val planterCheckInListToUpload: List<PlanterCheckInEntity> = dao.getPlanterCheckInsToUpload(planterInfoEntity.id)

        log("Found: ${planterCheckInListToUpload.size} planter check ins to upload")

        planterCheckInListToUpload.forEach { planterCheckIn ->

            log("Uploading planter check in image: ${planterCheckIn.localPhotoPath}")

            val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = planterCheckIn.localPhotoPath,
                                                                        lat = planterCheckIn.latitude,
                                                                        long = planterCheckIn.longitude))
            planterCheckIn.photoUrl = imageUrl
            dao.updatePlanterCheckIn(planterCheckIn)
        }
    }

}