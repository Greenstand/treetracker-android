package org.greenstand.android.TreeTracker.usecases

import java.io.File
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.managers.UserManager

class DeleteOldPlanterImagesUseCase(
    private val dao: TreeTrackerDAO,
    private val userManager: UserManager
) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {

        val planterCheckIns = dao.getAllPlanterCheckIn()

        // Delete all local image files for registrations except for the currently logged in users photo...
        val loggedOutPlanterCheckIns = planterCheckIns
            .filter { it.id != userManager.planterCheckinId }
            .sortedBy { it.createdAt }

        loggedOutPlanterCheckIns.forEach {
            val photoFile = File(it.localPhotoPath)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }

        dao.removePlanterCheckInLocalImagePaths(loggedOutPlanterCheckIns.map { it.id })
    }
}
