package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class SignupFlowScreen {
    EMAIL_PHONE,
    NAME
}

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val emailPhone: String? = null,
    val name: String? = null,
    val photoPath: String? = null,
)

class SignupViewModel : ViewModel() {

    private val _state = MutableLiveData(SignUpState())
    val state: LiveData<SignUpState> = _state

    fun setName(name: String) {
        // TODO validate data and show errors if needed
        _state.value = _state.value?.copy(name = name)
    }

    fun setEmailPhone(emailPhone: String) {
        // TODO validate data and show errors if needed
        _state.value = _state.value?.copy(emailPhone = emailPhone)
    }

    fun setPhotoPath(photoPath: String) {
        _state.value = _state.value?.copy(photoPath = photoPath)
    }

}
