package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.managers.UserManager
import timber.log.Timber

data class UploadPlanterParams(val planterInfoIds: List<Long>)


/**
 * Given a planter info ID this will upload the planters info
 * as well as each of the planters check in photos
 */
class UploadPlanterUseCase(private val dao: TreeTrackerDAO,
                           private val uploadPlanterInfoUserCase: UploadPlanterInfoUseCase,
                           private val userManager: UserManager,
                           private val removeLocalImagesWithIdsUseCase: RemoveLocalImagesWithIdsUseCase,
                           private val uploadPlanterCheckInUseCase: UploadPlanterCheckInUseCase) : UseCase<UploadPlanterParams, Unit>() {

    private fun log(msg: String) = Timber.tag("UploadPlanterUseCase").d(msg)

    override suspend fun execute(params: UploadPlanterParams) = withContext(Dispatchers.IO) {
        params.planterInfoIds

        // Upload all the images
        val planterCheckIns = dao.getPlanterCheckInsById(params.planterInfoIds)
        uploadPlanterCheckInUseCase.execute(UploadPlanterCheckInParams(planterCheckIns.map { it.id }))

        // Upload the user data
        val planterInfoList = params.planterInfoIds.mapNotNull { dao.getPlanterInfoById(it) }
        uploadPlanterInfoUserCase.execute(UploadPlanterInfoParams(planterInfoIds = planterInfoList.map { it.id }))

        // Delete all local image files for registations except for the currently logged in users photo...
        val loggedInPlanterCheckInsExceptRecent = planterCheckIns.filter { it.planterInfoId == userManager.userId }
            .sortedBy { it.createdAt }
            .let { it.subList(0, it.size) }

        val loggedOutPlanterCheckIns = planterCheckIns.filter { it.planterInfoId != userManager.userId }

        val planterCheckInIdsToDeleteLocalImages = (loggedInPlanterCheckInsExceptRecent + loggedOutPlanterCheckIns)
            .map { it.id }
        removeLocalImagesWithIdsUseCase.execute(RemoveLocalImagesWithIdsParams(planterCheckInIdsToDeleteLocalImages))
    }

}