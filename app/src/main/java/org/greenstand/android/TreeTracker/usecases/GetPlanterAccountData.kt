package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.PlanterAccountEntity
import timber.log.Timber
import java.util.*

const val TAG: String = "GetPlanterAccountData"

class GetPlanterAccountData(
    private val api: RetrofitApi,
    private val dao: TreeTrackerDAO
): UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {

        val planters = dao.getAllPlanterInfo().map { it.identifier }.toSet()
        val plantersAccountData = api.getPlanterAccountInfo(planters)
        if (plantersAccountData == null) {
            Timber.tag(TAG).i("No planter account data found for identifiers $planters")
            return
        }
        plantersAccountData.forEach { planterAccountData ->
            val planterAccountEntity = PlanterAccountEntity(
                planterInfoId = planterAccountData.planterIdentifier,
                uploadedTreeCount = planterAccountData.uploadedTrees,
                validatedTreeCount = planterAccountData.validatedTrees,
                paymentAmountPending = planterAccountData.pendingPayments,
                totalAmountPaid = planterAccountData.totalPayments,
                updatedAt = Calendar.getInstance().timeInMillis
            )
            dao.insertPlanterAccount(planterAccountEntity)
        }
    }
}