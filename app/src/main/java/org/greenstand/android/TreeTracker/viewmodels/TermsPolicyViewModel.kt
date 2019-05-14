package org.greenstand.android.TreeTracker.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.data.UserInfo
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.managers.PlanterManager
import org.greenstand.android.TreeTracker.utilities.Utils
import java.util.*

class TermsPolicyViewModel(private val planterManager: PlanterManager) : CoroutineViewModel() {

    lateinit var userInfo: UserInfo
    var photoPath: String? = null
        set(value) {
            field = value
            if (!field.isNullOrEmpty()) {
                confirm(onNavigateToMap)
            }
        }

    var onNavigateToMap: () -> Unit = { }

    private fun confirm(onConfirmationComplete: () -> Unit) {
        launch(Dispatchers.IO) {

            planterManager.addPlanterIdentification(userInfo.identification, photoPath!!)

            val planterDetailsId = planterManager.addPlanterDetails(userInfo.identification,
                                                                    userInfo.firstName,
                                                                    userInfo.lastName,
                                                                    userInfo.organization,
                                                                    Utils.dateFormat.format(Date()))

            planterManager.updateIdentifierId(userInfo.identification, planterDetailsId)

            withContext(Dispatchers.Main) { onConfirmationComplete() }
        }
    }

}