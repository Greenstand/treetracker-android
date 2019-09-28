package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.managers.UserManager
import java.io.File

class DeleteOldPlanterImagesUseCase(private val dao: TreeTrackerDAO,
                                    private val userManager: UserManager) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {

        val planterCheckIns = dao.getAllPlanterCheckIn()

        // Delete all local image files for registations except for the currently logged in users photo...
        val loggedOutPlanterCheckIns = planterCheckIns
            .filter { it.planterInfoId != userManager.planterCheckinId }
            .sortedBy { it.createdAt }
            .let { it.subList(0, it.size) }

        loggedOutPlanterCheckIns.forEach {
            val photoFile = File(it.localPhotoPath)
            if (photoFile.exists()) {
                photoFile.delete()
            }
        }

        dao.removePlanterCheckInLocalImagePaths(loggedOutPlanterCheckIns.map { it.id })

    }
}