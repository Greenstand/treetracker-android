package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.utilities.Validation

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val organization: String? = null,
    val photoPath: String? = null,
    val nameEntryStage: Boolean = false,
    val isEmailValid: Boolean = false,
    val isPhoneValid: Boolean = false,
    val canGoToNextScreen: Boolean = false,
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
    fun updateOrganization(organization: String) {
        _state.value = _state.value?.copy(organization = organization)
    }

    fun updateEmail(email: String) {
        // TODO validate data and show errors if needed, after click
        _state.value = _state.value?.copy(email = email)
        _state.value = _state.value?.copy(isEmailValid = Validation.isEmailValid(email))
    }

    fun updatePhone(phone: String) {
        _state.value = _state.value?.copy(phone = phone)
        _state.value = _state.value?.copy(isPhoneValid = Validation.isValidPhoneNumber(phone))

    }

    fun updateCredentialType(updatedCredential: Credential) {
        _state.value = _state.value?.copy(credential = updatedCredential)
    }

    fun updateSignUpState(state: Boolean){
        _state.value = _state.value?.copy(nameEntryStage = state)
    }

    private val currentIdentifier: String
        get() = _state.value?.credential?.text ?: ""

    suspend fun createUser(photoPath: String?): User? {
        if (photoPath != null) {
            val state: SignUpState = _state.value ?: return null
            val userId = with(state) {
                users.createUser(
                    // TODO fix user data usage
                    firstName = extractName(name, true),
                    lastName = extractName(name, false),
                    phone = state.phone,
                    email = state.email,
                    identifier = extractIdentifier(state),
                    organization = organization,
                    photoPath = photoPath,
                    isPowerUser = users.getPowerUser() == null,
                )
            }
            return users.getUser(userId)
        }
        return null
    }

    private fun extractName(name: String?, isFirstName: Boolean): String {
        name ?: return ""

        val names = name.split(" ")
        if (names.size == 1) {
            return if (isFirstName) name else ""
        }

        return if (isFirstName) {
            names[0]
        } else {
            names[1]
        }
    }

    private fun extractIdentifier(state: SignUpState):String{
        return when {
            (!state.email.isNullOrBlank()) -> {
                state.email
            }(!state.phone.isNullOrBlank()) -> {
                state.phone
            }else -> {
                currentIdentifier
            }
        }
    }
}
