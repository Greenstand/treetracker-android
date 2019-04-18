package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.utilities.Validation.isEmailValid

class PlanterDetailsViewModel: CoroutineViewModel()  {

    var newPlanter: PlanterDetailsEntity? = null
    var planter: PlanterIdentificationsEntity? = null
    var planter_id: Long = -1
    var identifier: MutableLiveData<String>? = null

    fun isUserPresentOnDevice(userInputted: String): LiveData<PlanterDetailsEntity>{
        return TreeManager.getPlanterByInputtedText(userInputted)
    }

    fun addNewPlanter(){
        launch {
            newPlanter?.let {
                withContext(Dispatchers.IO) {
                    planter_id = TreeManager.insertPlanter(newPlanter?.identifier!!,newPlanter?.firstName!!,
                         newPlanter?.lastName!!,newPlanter?.organization!!, newPlanter?.phone!!, newPlanter?.email!!,
                        newPlanter?.uploaded!!, newPlanter?.timeCreated!!)
                }
            }

        }
    }

    fun addPlanterIdentifications(){
        launch {
            planter?.let {
                withContext(Dispatchers.IO) {
                    TreeManager.insertPlanterIdentification(planter?.id!!.toLong(),planter?.identifier!!, planter?.photoPath!!,
                        planter?.photoUrl!!, planter?.timeCreated!!)
                }
            }

        }
    }
}