package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Users

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val emailText: String = "",
    val phoneText: String = "",
    val name: String? = null,
    val photoPath: String? = null,
    val credentialType: CredentialType = CredentialType.Email,
    val showEmailText: Boolean = true,
    val showPhoneText: Boolean = false,

)

enum class CredentialType {
    Phone,
    Email
}

class SignupViewModel(private val users: Users) : ViewModel() {

    private val _state = MutableLiveData(SignUpState())
    val state: LiveData<SignUpState> = _state

    /**
     * Note: There needs to be some type of validation to ensure that the user
     * enters an email and that the email CredentialType has been properly selected.
     * Same with the phone.
     *
     * We can't have the user enter an email when selecting the phone credential
     */

    fun updateName(name: String) {
        // TODO validate data and show errors if needed, after click
        _state.value = _state.value?.copy(name = name)
    }

    fun updateEmail(email: String) {
        // TODO validate data and show errors if needed, after click
        _state.value = _state.value?.copy(emailText = email)
    }

    fun updatePhone(phone: String) {
        _state.value = _state.value?.copy(phoneText = phone)
    }

    fun updateCredentialType(updatedType: CredentialType) {
        _state.value = _state.value?.copy(credentialType = updatedType)
    }

    private val currentIdentifier: String
        get() = _state.value?.emailText ?: _state.value?.phoneText ?: ""

    suspend fun setPhotoPath(photoPath: String?): Boolean {
        if (photoPath != null) {
            val state: SignUpState = _state.value ?: return false
            with(state) {
                users.createUser(
                    // TODO fix user data usage
                    firstName = name?.split(" ")?.get(0) ?: "",
                    lastName = name?.split(" ")?.get(0) ?: "",
                    phone = phoneText,
                    email = emailText,
                    identifier = currentIdentifier,
                    organization = null,
                    photoPath = photoPath,
                )
            }
            return true
        }
        return false
    }
}
