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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.utilities.Validation
import org.greenstand.android.TreeTracker.utils.ValidationUtils
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

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
    abstract var text: String
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

sealed class SignupAction : Action {
    data class UpdateFirstName(
        val firstName: String?,
    ) : SignupAction()

    data class UpdateLastName(
        val lastName: String?,
    ) : SignupAction()

    data class UpdateEmail(
        val email: String,
    ) : SignupAction()

    data class UpdatePhone(
        val phone: String,
    ) : SignupAction()

    data class UpdateCredentialType(
        val credential: Credential,
    ) : SignupAction()

    object SubmitInfo : SignupAction()

    object CloseExistingUserDialog : SignupAction()

    data class UpdateSelfieTutorialDialog(
        val show: Boolean,
    ) : SignupAction()

    object EnableAutofocus : SignupAction()

    object GoToCredentialEntry : SignupAction()

    object ClosePrivacyPolicyDialog : SignupAction()

    data class SetExistingUserAsPowerUser(
        val id: Long,
    ) : SignupAction()

    object NavigateBack : SignupAction()

    object LaunchCamera : SignupAction()

    data class ExistingUserSelected(
        val user: User,
    ) : SignupAction()
}

class SignupViewModel(
    private val userRepo: UserRepo,
    private val checkForInternetUseCase: CheckForInternetUseCase,
) : BaseViewModel<SignUpState, SignupAction>(SignUpState()) {
    init {
        viewModelScope.launch(Dispatchers.Main) {
            val result = checkForInternetUseCase.execute(Unit)
            val initialSetupRequired = isInitialSetupRequired()
            updateState {
                copy(
                    isInternetAvailable = result,
                    showSelfieTutorial = initialSetupRequired,
                    isTherePowerUser = !initialSetupRequired,
                )
            }
        }
    }

    override fun handleAction(action: SignupAction) {
        when (action) {
            is SignupAction.UpdateFirstName ->
                updateName(action.firstName) { filtered, error ->
                    copy(firstName = filtered, firstNameError = if (filtered.isNullOrEmpty()) null else error)
                }
            is SignupAction.UpdateLastName ->
                updateName(action.lastName) { filtered, error ->
                    copy(lastName = filtered, lastNameError = if (filtered.isNullOrEmpty()) null else error)
                }
            is SignupAction.UpdateEmail -> {
                updateState {
                    copy(
                        email = action.email.lowercase(),
                        phone = null,
                        isCredentialValid = action.email.contains('@'),
                    )
                }
            }
            is SignupAction.UpdatePhone -> {
                updateState {
                    copy(
                        phone = action.phone,
                        email = null,
                        isCredentialValid = Validation.isValidPhoneNumber(action.phone),
                    )
                }
            }
            is SignupAction.UpdateCredentialType -> {
                updateState { copy(credential = action.credential) }
            }
            is SignupAction.SubmitInfo -> submitInfo()
            is SignupAction.CloseExistingUserDialog -> {
                updateState { copy(existingUser = null) }
            }
            is SignupAction.UpdateSelfieTutorialDialog -> {
                updateState { copy(showSelfieTutorial = action.show) }
            }
            is SignupAction.EnableAutofocus -> {
                updateState { copy(autofocusTextEnabled = true) }
            }
            is SignupAction.GoToCredentialEntry -> {
                updateState {
                    copy(
                        isCredentialView = true,
                        firstName = null,
                        lastName = null,
                        canGoToNextScreen = true,
                    )
                }
            }
            is SignupAction.ClosePrivacyPolicyDialog -> {
                updateState { copy(showPrivacyDialog = false) }
            }
            is SignupAction.SetExistingUserAsPowerUser -> {
                viewModelScope.launch {
                    userRepo.setPowerUserStatus(action.id, true)
                }
            }
            else -> { }
        }
    }

    private fun updateName(
        name: String?,
        applyUpdate: SignUpState.(String?, String?) -> SignUpState,
    ) {
        val filtered = name?.let { ValidationUtils.filterNameInput(it) }
        val (_, error) = ValidationUtils.validateName(filtered)
        updateState { applyUpdate(filtered, error) }
    }

    private fun submitInfo() {
        val credential = extractIdentifier(currentState)
        viewModelScope.launch {
            if (userRepo.doesUserExists(credential)) {
                val existingUser = userRepo.getUserWithWallet(credential)
                updateState { copy(existingUser = existingUser) }
            } else {
                updateState { copy(isCredentialView = false, canGoToNextScreen = false) }
            }
        }
    }

    fun isFormValid(): Boolean {
        val state = currentState
        val (firstNameValid, _) = ValidationUtils.validateName(state.firstName)
        val (lastNameValid, _) = ValidationUtils.validateName(state.lastName)
        return firstNameValid && lastNameValid
    }

    suspend fun isInitialSetupRequired(): Boolean = userRepo.getPowerUser() == null

    suspend fun createUser(photoPath: String?): User? {
        if (photoPath != null) {
            val state = currentState
            val userId =
                userRepo.createUser(
                    firstName = state.firstName!!,
                    lastName = state.lastName!!,
                    phone = state.phone,
                    email = state.email,
                    wallet = extractIdentifier(state),
                    photoPath = photoPath,
                    isPowerUser = userRepo.getPowerUser() == null,
                )
            return userRepo.getUser(userId)
        }
        return null
    }

    private fun extractIdentifier(state: SignUpState): String =
        when (state.credential) {
            is Credential.Email -> state.email
            is Credential.Phone -> state.phone
        } ?: "DEFAULT"
}