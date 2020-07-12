package org.greenstand.android.TreeTracker.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.usecases.PlanterCheckInParams
import org.greenstand.android.TreeTracker.usecases.PlanterCheckInUseCase
import org.greenstand.android.TreeTracker.utilities.Validation

class LoginViewModel(private val dao: TreeTrackerDAO,
                     private val planterCheckInUseCase: PlanterCheckInUseCase): ViewModel()  {

    private var email: String? = null
    private var phone: String? = null

    var photoPath: String? = null
        set(value) {
            field = value
            if (!field.isNullOrEmpty()) {
                confirm(onNavigateToMap)
            }
        }

    var onNavigateToMap: () -> Unit = { }

    private val errorMessageMutableLiveData = MutableLiveData<Int>()
    private val loginButtonStateMutableLiveData = MutableLiveData<Boolean>().apply {
        value = false
    }

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
            loginButtonStateMutableLiveData.value = true
        } else {
            if (!Validation.isEmailValid(email.orEmpty())) {
                loginButtonStateMutableLiveData.value = false
            }
            //errorMessageMutableLiveData.value = R.string.invalid_identification
        }
    }

    fun isUserPresentOnDevice(): Boolean {
        return dao.getPlanterInfoIdByIdentifier(userIdentification) != null
    }

    private fun confirm(onConfirmationComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {

            planterCheckInUseCase.execute(PlanterCheckInParams(localPhotoPath = photoPath!!,
                                                               identifier = userIdentification))

            onConfirmationComplete()
        }
    }
}