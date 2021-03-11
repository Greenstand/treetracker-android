package org.greenstand.android.TreeTracker.login

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.usecases.PlanterCheckInParams
import org.greenstand.android.TreeTracker.usecases.PlanterCheckInUseCase
import timber.log.Timber

class LoginViewModel(
    private val dao: TreeTrackerDAO,
    private val planterCheckInUseCase: PlanterCheckInUseCase,
    private val analytics: Analytics
) : ViewModel() {

//    private var email: String? = null
//    private var phone: String? = null

    private lateinit var userCredentials: String

    var photoPath: String? = null
        set(value) {
            field = value

            field?.let { path ->
                if (path.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        planterCheckInUseCase.execute(
                            PlanterCheckInParams(localPhotoPath = path, identifier = userCredentials)
                        )
                    }
                }
            }
        }

    private val _uiEvents = MutableLiveData<UIEvent>()
    val uiEvents: LiveData<UIEvent> = _uiEvents

    /**
     * User's information. Email will always be used over phone when possible.
     */
//    val userIdentification: String
//        get() = email ?: phone!!

//    fun updateEmail(email: String) {
//        val trimmedEmail = email.trim()
//        if (Validation.isEmailValid(trimmedEmail)) {
//            this.email = trimmedEmail
//        } else {
//            if (!Validation.isValidPhoneNumber(phone.orEmpty())) {
//            }
//            //errorMessageMutableLiveData.value = R.string.invalid_identification
//        }
//    }

//    fun updatePhone(phone: String) {
//        val trimmedPhone = phone.trim()
//        if (Validation.isValidPhoneNumber(trimmedPhone)) {
//            this.phone = trimmedPhone
//        } else {
//            if (!Validation.isEmailValid(email.orEmpty())) {
//            }
//            //errorMessageMutableLiveData.value = R.string.invalid_identification
//        }
//    }

//    private suspend fun isUserPresentOnDevice(credentials: String): Boolean {
//        return dao.getPlanterInfoIdByIdentifier(credentials) != null
//    }

//    private fun confirm(onConfirmationComplete: () -> Unit) {
//        viewModelScope.launch(Dispatchers.Main) {
//
//            planterCheckInUseCase.execute(PlanterCheckInParams(localPhotoPath = photoPath!!,
//                                                               identifier = userIdentification))
//
//            onConfirmationComplete()
//        }
//    }

    /**
     * @param credentials - The user provided email or phone.
     */
    fun loginButtonClicked(credentials: String) {

        this.userCredentials = credentials

        this.viewModelScope.launch {

            if (dao.getPlanterInfoIdByIdentifier(credentials) != null) {
                _uiEvents.postValue(UIEvent.TakePhotoEvent)
                Timber.d("User already on device, going to map")
            } else {
                Timber.d("User not on device, going to signup flow")
                analytics.userEnteredEmailPhone()
                _uiEvents.postValue(
                    UIEvent.NavigationRequestEvent(
                        LoginFragmentDirections.actionLoginFragmentToSignUpFragment(credentials)
                    )
                )
            }
        }
    }

    sealed class UIEvent {
        data class NavigationRequestEvent(val newNavDirection: NavDirections) : UIEvent()
        data class ErrorEvent(@StringRes val errorMessage: Int) : UIEvent()
        object TakePhotoEvent : UIEvent()
    }
}
