package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.utilities.Validation

class LoginViewModel: CoroutineViewModel()  {

    private var email: String? = null
    private var phone: String? = null

    private val errorMessageMutableLiveData = MutableLiveData<Int>()
    private val loginButtonStateMutableLiveData = MutableLiveData<Boolean>()

    val errorMessageLiveDate: LiveData<Int> = errorMessageMutableLiveData
    val loginButtonStateLiveDate: LiveData<Boolean> = loginButtonStateMutableLiveData

    val userIdentification: String
        get() {
            // We always use email over phone if possible
            return email ?: phone!!
        }

    fun updateEmail(email: String) {
        if (Validation.isEmailValid(email)) {
            this.email = email
            loginButtonStateMutableLiveData.value = true
        } else {
            if (!Validation.isValidPhoneNumber(phone.orEmpty())) {
                loginButtonStateMutableLiveData.value = false
            }
            //errorMessageMutableLiveData.value = R.string.invalid_identification
        }
    }

    fun updatePhone(phone: String) {
        if (Validation.isValidPhoneNumber(phone)) {
            this.phone = phone
            loginButtonStateMutableLiveData.value = false
        } else {
            if (!Validation.isEmailValid(email.orEmpty())) {
                loginButtonStateMutableLiveData.value = false
            }
            //errorMessageMutableLiveData.value = R.string.invalid_identification
        }
    }

    fun isUserPresentOnDevice(): Boolean {

        val userIdentification: String = email ?: phone ?: return false

        return TreeManager.getPlanterByInputtedText(userIdentification) != null
    }
//
//    fun addNewPlanter(){
//        launch {
//            newPlanter?.let {
//                withContext(Dispatchers.IO) {
//                    planter_id = TreeManager.insertPlanter(newPlanter?.identifier!!,newPlanter?.firstName!!,
//                         newPlanter?.lastName!!,newPlanter?.organization!!, newPlanter?.phone!!, newPlanter?.email!!,
//                        newPlanter?.uploaded!!, newPlanter?.timeCreated!!)
//                }
//            }
//
//        }
//    }
//
//    fun addPlanterIdentifications(){
//        launch {
//            planter?.let {
//                withContext(Dispatchers.IO) {
//                    TreeManager.insertPlanterIdentification(planter?.id!!.toLong(),planter?.identifier!!, planter?.photoPath!!,
//                        planter?.photoUrl!!, planter?.timeCreated!!)
//                }
//            }
//
//        }
//    }
}