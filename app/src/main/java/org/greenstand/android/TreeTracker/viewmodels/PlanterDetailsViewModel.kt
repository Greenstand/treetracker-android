package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.utilities.Validation.isEmailValid

class PlanterDetailsViewModel: CoroutineViewModel()  {

    val phoneNumber = MutableLiveData<String>()
    val emailAddress = MutableLiveData<String>()

    fun phoneNumberEntered(input: String){
        phoneNumber.value = input
    }

    fun emailAddressEntered(input: String){
        emailAddress.value = input
    }
    fun isNumberValid(phoneNumber: String) = if(!phoneNumber.matches(Regex("^\\+[0-9]{10,13}\$"))) true else false

    fun isUserPresentOnDevice(userInputted: String): LiveData<PlanterDetailsEntity>{
        return TreeManager.getPlanterByInputtedText(userInputted)
    }
}