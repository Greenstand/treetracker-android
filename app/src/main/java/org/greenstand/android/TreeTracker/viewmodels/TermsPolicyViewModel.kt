package org.greenstand.android.TreeTracker.viewmodels

import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.data.UserInfo
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.managers.PlanterManager
import org.greenstand.android.TreeTracker.utilities.Utils
import java.util.*

class TermsPolicyViewModel : CoroutineViewModel() {

    lateinit var userInfo: UserInfo
    var photoPath: String? = null

    fun confirm() {
        launch {

            val planterIdentificationsId = PlanterManager.addPlanterIdentification(userInfo.identification, "PHOTO_PATH")


            val planterDetailsId = PlanterManager.addPlanterDetails(userInfo.identification,
                                                                    userInfo.firstName,
                                                                    userInfo.lastName,
                                                                    userInfo.organization,
                                                                    Utils.dateFormat.format(Date()))

            PlanterManager.updateIdentifierId(userInfo.identification, planterIdentificationsId)
        }
    }

}