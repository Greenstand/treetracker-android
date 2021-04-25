package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.utilities.Validation

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val name: String? = null,
    val photoPath: String? = null,
    val credential: Credential = Credential.Email(),
)

sealed class Credential {

    /**
     * The actual credential value
     */
    abstract var text: String

    /**
     * Whether or not this credential is valid
     */
    abstract val isValid: Boolean

    class Email : Credential() {

        override var text: String = ""

        override val isValid: Boolean
            get() = Validation.isEmailValid(text)
    }

    class Phone : Credential() {

        override var text: String = ""

        override val isValid: Boolean
            get() = Validation.isValidPhoneNumber(text)
    }
}

class SignupViewModel(private val users: Users) : ViewModel() {

    private val _state = MutableLiveData(SignUpState())
    val state: LiveData<SignUpState> = _state

    fun updateName(name: String) {
        // TODO validate data and show errors if needed, after click
        _state.value = _state.value?.copy(name = name)
    }

    fun updateEmail(email: String) {
        // TODO validate data and show errors if needed, after click
        _state.value = _state.value?.copy(credential = Credential.Email().apply { text = email })
    }

    fun updatePhone(phone: String) {
        _state.value = _state.value?.copy(credential = Credential.Phone().apply { text = phone })
    }

    fun updateCredentialType(updatedCredential: Credential) {
        _state.value = _state.value?.copy(credential = updatedCredential)
    }

    private val currentIdentifier: String
        get() = _state.value?.credential?.text ?: ""

    suspend fun setPhotoPath(photoPath: String?): Boolean {
        if (photoPath != null) {
            val state: SignUpState = _state.value ?: return false
            with(state) {
                users.createUser(
                    // TODO fix user data usage
                    firstName = name?.split(" ")?.get(0) ?: "",
                    lastName = name?.split(" ")?.get(0) ?: "",
                    phone = if (state.credential is Credential.Phone) {
                        state.credential.text
                    } else {
                        null
                    },
                    email = if (state.credential is Credential.Email) {
                        state.credential.text
                    } else {
                        null
                    },
                    identifier = currentIdentifier,
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
