/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utilities.Validation
import org.greenstand.android.TreeTracker.utils.ValidationUtils

// Dequeue breaks equals so state will not be updated when navigating
data class SignUpState(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val photoPath: String? = null,
    val isCredentialView: Boolean = true,
    val isCredentialValid: Boolean = false,
    val existingUser: User? = null,
    val canGoToNextScreen: Boolean = false,
    val credential: Credential = Credential.Phone(),
    val autofocusTextEnabled: Boolean = false,
    val isInternetAvailable: Boolean = false,
    val isTherePowerUser: Boolean? = null,
    val showSelfieTutorial: Boolean? = null,
    val showPrivacyDialog: Boolean? = true,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
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
    private val userRepo: UserRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase
) : ViewModel() {

    private val _state = MutableLiveData(SignUpState())
    val state: LiveData<SignUpState> = _state

    init {
        viewModelScope.launch(Dispatchers.Main) {
            val result = checkForInternetUseCase.execute(Unit)
            _state.value = _state.value?.copy(isInternetAvailable = result, showSelfieTutorial = isInitialSetupRequired(), isTherePowerUser = !isInitialSetupRequired())
        }
    }

    fun updateFirstName(firstName: String?) {
        val filtered = firstName?.let { ValidationUtils.filterNameInput(it) }
        val (isValid, error) = ValidationUtils.validateName(filtered)
        _state.value = _state.value?.copy(
            firstName = filtered,
            firstNameError = if (filtered.isNullOrEmpty()) null else error
        )
    }

    fun updateLastName(lastName: String?) {
        val filtered = lastName?.let { ValidationUtils.filterNameInput(it) }
        val (isValid, error) = ValidationUtils.validateName(filtered)
        _state.value = _state.value?.copy(
            lastName = filtered,
            lastNameError = if (filtered.isNullOrEmpty()) null else error
        )
    }
    fun isFormValid(): Boolean {
        val state = _state.value ?: return false
        val (firstNameValid, _) = ValidationUtils.validateName(state.firstName)
        val (lastNameValid, _) = ValidationUtils.validateName(state.lastName)
        return firstNameValid && lastNameValid
    }

    fun setExistingUserAsPowerUser(id: Long){
        viewModelScope.launch {
                userRepo.setPowerUserStatus(id, true)
        }
    }

    fun updateEmail(email: String) {
        _state.value = _state.value?.copy(
            email = email.lowercase(),
            phone = null,
            isCredentialValid = email.contains('@')
        )
    }

    fun updatePhone(phone: String) {
        _state.value = _state.value?.copy(
            phone = phone,
            email = null,
            isCredentialValid = Validation.isValidPhoneNumber(phone)
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
            if (userRepo.doesUserExists(credential)) {
                _state.value = _state.value?.copy(
                    existingUser = userRepo.getUserWithWallet(credential),
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

    fun updateSelfieTutorialDialog(state: Boolean) {
        _state.value = _state.value?.copy(showSelfieTutorial = state)
    }

    suspend fun isInitialSetupRequired(): Boolean = userRepo.getPowerUser() == null

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
            firstName = null,
            lastName = null,
            canGoToNextScreen = true,
        )
    }

    suspend fun createUser(photoPath: String?): User? {
        if (photoPath != null) {
            val userId = with(_state.value ?: return null) {
                userRepo.createUser(
                    firstName = firstName!!,
                    lastName = lastName!!,
                    phone = phone,
                    email = email,
                    wallet = extractIdentifier(this),
                    photoPath = photoPath,
                    isPowerUser = userRepo.getPowerUser() == null,
                )
            }
            return userRepo.getUser(userId)
        }
        return null
    }

    private fun extractIdentifier(state: SignUpState): String {
        return when (state.credential) {
            is Credential.Email -> state.email
            is Credential.Phone -> state.phone
        } ?: "DEFAULT"
    }

    fun closePrivacyPolicyDialog() {
        _state.value = _state.value?.copy(
            showPrivacyDialog = false,
        )
    }
}