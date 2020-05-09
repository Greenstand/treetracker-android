package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import timber.log.Timber

data class UploadPlanterParams(val planterInfoIds: List<Long>)


/**
 * Given a planter info ID this will upload the planters info
 * as well as each of the planters check in photos
 */
class UploadPlanterUseCase(private val dao: TreeTrackerDAO,
                           private val uploadPlanterInfoUseCase: UploadPlanterInfoUseCase,
                           private val deleteOldPlanterImagesUseCase: DeleteOldPlanterImagesUseCase,
                           private val uploadPlanterCheckInUseCase: UploadPlanterCheckInUseCase) : UseCase<UploadPlanterParams, Unit>() {

    private fun log(msg: String) = Timber.tag("UploadPlanterUseCase").d(msg)

    override suspend fun execute(params: UploadPlanterParams) = withContext(Dispatchers.IO) {
        params.planterInfoIds

        // Upload all the images
        val planterCheckIns = dao.getPlanterCheckInsById(params.planterInfoIds)
        uploadPlanterCheckInUseCase.execute(UploadPlanterCheckInParams(planterCheckIns.map { it.id }))

        // Upload the user data
        val planterInfoList = params.planterInfoIds.mapNotNull { dao.getPlanterInfoById(it) }
        uploadPlanterInfoUseCase.execute(UploadPlanterInfoParams(planterInfoIds = planterInfoList.map { it.id }))

        // Delete local images
        deleteOldPlanterImagesUseCase.execute(Unit)
    }

}