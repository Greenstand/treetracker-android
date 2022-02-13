package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.Users
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utilities.Validation

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val photoPath: String? = null,
    val isCredentialView: Boolean = true,
    val isEmailValid: Boolean = false,
    val isPhoneValid: Boolean = false,
    val existingUser: User? = null,
    val canGoToNextScreen: Boolean = false,
    val credential: Credential = Credential.Email(),
    val autofocusTextEnabled: Boolean = false,
    val isInternetAvailable: Boolean = false
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

class SignupViewModel(
    private val users: Users,
    private val checkForInternetUseCase: CheckForInternetUseCase
) : ViewModel() {

    private val _state = MutableLiveData(SignUpState())
    val state: LiveData<SignUpState> = _state

    init {
        viewModelScope.launch(Dispatchers.Main) {
            val result = checkForInternetUseCase.execute(Unit)
            _state.value = _state.value?.copy(isInternetAvailable = result)
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value?.copy(name = name)
    }

    fun updateEmail(email: String) {
        _state.value = _state.value?.copy(
            email = email,
            isEmailValid = Validation.isEmailValid(email)
        )
    }

    fun updatePhone(phone: String) {
        _state.value = _state.value?.copy(
            phone = phone,
            isPhoneValid = Validation.isValidPhoneNumber(phone)
        )
    }

    fun updateCredentialType(updatedCredential: Credential) {
        _state.value = _state.value?.copy(credential = updatedCredential)
    }

    /**
     *  update _state according to user existence
     */
    fun submitInfo() {
        val credential = _state.value?.let { extractIdentifier(it) }!!

        viewModelScope.launch {
            if (users.doesUserExists(credential)) {
                _state.value = _state.value?.copy(
                    existingUser = users.getUserWithWallet(credential),
                )
            } else {
                goToNameEntry()
            }
        }
    }

    fun closeExistingUserDialog() {
        _state.value = _state.value?.copy(
            existingUser = null,
        )
    }

    fun enableAutofocus() {
        _state.value = _state.value?.copy(autofocusTextEnabled = true)
    }

    private fun goToNameEntry() {
        _state.value = _state.value?.copy(
            isCredentialView = false,
            canGoToNextScreen = false,
        )
    }

    fun goToCredentialEntry() {
        _state.value = _state.value?.copy(
            isCredentialView = true,
            name = null,
            canGoToNextScreen = true,
        )
    }

    suspend fun createUser(photoPath: String?): User? {
        if (photoPath != null) {
            val userId = with(_state.value ?: return null) {
                users.createUser(
                    firstName = extractName(name, true),
                    lastName = extractName(name, false),
                    phone = phone,
                    email = email,
                    wallet = extractIdentifier(this),
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

    private fun extractIdentifier(state: SignUpState): String {
        return when(state.credential) {
            is Credential.Email -> state.email
            is Credential.Phone -> state.phone
        } ?: "DEFAULT"
    }
}
