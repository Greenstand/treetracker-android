package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO

data class PlanterCheckInParams(
    val identifier: String,
    val localPhotoPath: String
)

class PlanterCheckInUseCase(
    private val createPlanterCheckInUseCase: CreatePlanterCheckInUseCase,
    private val dao: TreeTrackerDAO
) :
    UseCase<PlanterCheckInParams, Unit>() {

    override suspend fun execute(params: PlanterCheckInParams) {
        withContext(Dispatchers.IO) {
            val planterInfoId = dao.getPlanterInfoIdByIdentifier(params.identifier)

            planterInfoId ?: throw IllegalStateException("Planter ID should not be null")

            createPlanterCheckInUseCase.execute(
                CreatePlanterCheckInParams(
                    planterInfoId = planterInfoId,
                    localPhotoPath = params.localPhotoPath
                )
            )
        }
    }
}
