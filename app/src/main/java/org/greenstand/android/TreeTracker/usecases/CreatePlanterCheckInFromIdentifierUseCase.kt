package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO


data class PlanterCheckInParams(val identifier: String,
                                val localPhotoPath: String)

class PlanterCheckInUseCase(private val createPlanterCheckInUseCase: CreatePlanterCheckInUseCase,
                            private val dao: TreeTrackerDAO) : UseCase<PlanterCheckInParams, Unit>() {

    override suspend fun execute(params: PlanterCheckInParams) {

        val planterInfoId = dao.getPlanterInfoIdByIdentifier(params.identifier)

        createPlanterCheckInUseCase.execute(CreatePlanterCheckInParams(planterInfoId = planterInfoId,
                                                                       localPhotoPath = params.localPhotoPath))

    }
}