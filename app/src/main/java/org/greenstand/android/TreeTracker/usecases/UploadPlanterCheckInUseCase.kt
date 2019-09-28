package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import timber.log.Timber

data class UploadPlanterCheckInParams(val planterCheckInIds: List<Long>)

class UploadPlanterCheckInUseCase(private val dao: TreeTrackerDAO,
                                  private val uploadImageUseCase: UploadImageUseCase) : UseCase<UploadPlanterCheckInParams, Unit>() {

    private fun log(msg: String) = Timber.tag("UploadPlanterCheckInUseCase").d(msg)

    override suspend fun execute(params: UploadPlanterCheckInParams) {

        val planterCheckInListToUpload = dao.getPlanterCheckInsById(params.planterCheckInIds)
            .filter { it.photoUrl == null }

        log("Found: ${planterCheckInListToUpload.size} planter check ins to upload")

        planterCheckInListToUpload.forEach { planterCheckIn ->

            log("Uploading planter check in image: ${planterCheckIn.localPhotoPath}")

            val imageUrl = uploadImageUseCase.execute(UploadImageParams(imagePath = planterCheckIn.localPhotoPath!!,
                                                                        lat = planterCheckIn.latitude,
                                                                        long = planterCheckIn.longitude))
            planterCheckIn.photoUrl = imageUrl
            dao.updatePlanterCheckIn(planterCheckIn)
        }
    }

}