package org.greenstand.android.TreeTracker.usecases

import java.io.File
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.User

class DeleteOldPlanterImagesUseCase(
    private val dao: TreeTrackerDAO,
    private val user: User
) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {

        val planterCheckIns = dao.getPlanterCheckInsToUpload()

        // Delete all local image files for registrations except for the currently logged in users photo...
        val loggedOutPlanterCheckIns = planterCheckIns
            .filter {
                it.id != user.planterCheckinId &&
                    it.localPhotoPath != null && it.photoUrl != null
            }
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
