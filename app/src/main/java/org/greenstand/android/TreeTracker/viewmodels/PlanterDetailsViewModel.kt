package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.utilities.Validation.isEmailValid

class PlanterDetailsViewModel: CoroutineViewModel()  {

    var newPlanter: PlanterDetailsEntity? = null

    /*This is code for further improvement*/
//    val phoneNumber = MutableLiveData<String>()
//    val emailAddress = MutableLiveData<String>()
//
//    fun phoneNumberEntered(input: String){
//        phoneNumber.value = input
//    }
//
//    fun emailAddressEntered(input: String){
//        emailAddress.value = input
//    }
//    fun isNumberValid(phoneNumber: String) = if(!phoneNumber.matches(Regex("^\\+[0-9]{10,13}\$"))) true else false

    fun isUserPresentOnDevice(userInputted: String): LiveData<PlanterDetailsEntity>{
        return TreeManager.getPlanterByInputtedText(userInputted)
    }

    fun addNewPlanter(){
        launch {
            newPlanter?.let {
                withContext(Dispatchers.IO) {
                    val planter_id = TreeManager.insertPlanter(newPlanter?.identifier!!,newPlanter?.firstName!!,
                         newPlanter?.lastName!!,newPlanter?.organization!!, newPlanter?.phone!!, newPlanter?.email!!,
                        newPlanter?.uploaded!!, newPlanter?.timeCreated!!)
                }
            }

        }
    }
}