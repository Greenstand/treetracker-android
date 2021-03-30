package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Users

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val emailPhone: String? = null,
    val name: String? = null,
    val photoPath: String? = null,
)

class SignupViewModel(private val users: Users) : ViewModel() {

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

    suspend fun setPhotoPath(photoPath: String?): Boolean {
        if (photoPath != null) {
            val state: SignUpState = _state.value ?: return false
            with(state) {
                users.createUser(
                    // TODO fix user data usage
                    firstName = name?.split(" ")?.get(0) ?: "",
                    lastName = name?.split(" ")?.get(0) ?: "",
                    phone = emailPhone,
                    email = emailPhone,
                    identifier = emailPhone ?: "",
                    organization = null,
                    photoPath = photoPath,
                    isPowerUser = false
                )
            }
            return true
        }
        return false
    }

}
