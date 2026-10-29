/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.uitest.signup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.signup.Credential
import org.greenstand.android.TreeTracker.signup.CredentialEntryView
import org.greenstand.android.TreeTracker.signup.SignUp
import org.greenstand.android.TreeTracker.signup.SignUpState
import org.greenstand.android.TreeTracker.signup.SignupAction
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialEntryUiTest : ComposeUiTest() {
    private val actions = mutableListOf<SignupAction>()

    private val existingUser =
        User(
            id = 7L,
            wallet = "0712345678",
            numberOfTrees = 3,
            firstName = "Jane",
            lastName = "Doe",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false,
        )

    private fun setCredentialEntry(state: SignUpState) {
        setContent {
            CredentialEntryView(state = state, onHandleAction = { actions += it })
        }
    }

    private fun textField() = composeTestRule.onNode(hasSetTextAction())

    @Test
    fun `sign up shows credential entry in credential view`() {
        setContent { SignUp(state = SignUpState(showPrivacyDialog = false)) }

        composeTestRule.onNodeWithText(string(R.string.phone_placeholder).uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.email_placeholder).uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertDoesNotExist()
    }

    @Test
    fun `sign up shows name entry outside the credential view`() {
        setContent { SignUp(state = SignUpState(isCredentialView = false)) }

        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.last_name_hint)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.phone_placeholder).uppercase()).assertDoesNotExist()
    }

    @Test
    fun `privacy dialog is shown by default and hides the navigation buttons`() {
        setCredentialEntry(SignUpState())

        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `accepting the privacy policy sends ClosePrivacyPolicyDialog`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = true))

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        assertEquals(listOf<SignupAction>(SignupAction.ClosePrivacyPolicyDialog), actions)
    }

    @Test
    fun `navigation buttons are shown once the privacy dialog is closed`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false))

        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertExists()
    }

    @Test
    fun `clicking email switches the credential type to email`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false))

        composeTestRule.onNodeWithText(string(R.string.email_placeholder).uppercase()).performClick()

        val update = actions.filterIsInstance<SignupAction.UpdateCredentialType>().single()
        assertTrue(update.credential is Credential.Email)
    }

    @Test
    fun `clicking phone switches the credential type to phone`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, credential = Credential.Email()))

        composeTestRule.onNodeWithText(string(R.string.phone_placeholder).uppercase()).performClick()

        val update = actions.filterIsInstance<SignupAction.UpdateCredentialType>().single()
        assertTrue(update.credential is Credential.Phone)
    }

    @Test
    fun `phone credential shows the phone placeholder and sends UpdatePhone`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false))
        composeTestRule.onNodeWithText(string(R.string.phone_placeholder)).assertExists()

        textField().performTextInput("0712345678")

        assertEquals(
            listOf(SignupAction.UpdatePhone("0712345678")),
            actions.filterIsInstance<SignupAction.UpdatePhone>(),
        )
        assertTrue(actions.none { it is SignupAction.UpdateEmail })
    }

    @Test
    fun `email credential shows the email placeholder and sends UpdateEmail`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, credential = Credential.Email()))
        composeTestRule.onNodeWithText(string(R.string.email_placeholder)).assertExists()

        textField().performTextInput("jane@example.org")

        assertEquals(
            listOf(SignupAction.UpdateEmail("jane@example.org")),
            actions.filterIsInstance<SignupAction.UpdateEmail>(),
        )
        assertTrue(actions.none { it is SignupAction.UpdatePhone })
    }

    @Test
    fun `text field shows the entered phone number`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, phone = "0712345678"))

        composeTestRule.onNodeWithText("0712345678").assertIsDisplayed()
    }

    @Test
    fun `text field shows the entered email`() {
        setCredentialEntry(
            SignUpState(
                showPrivacyDialog = false,
                credential = Credential.Email(),
                email = "jane@example.org",
            ),
        )

        composeTestRule.onNodeWithText("jane@example.org").assertIsDisplayed()
    }

    @Test
    fun `focusing the text field sends EnableAutofocus`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false))

        textField().performClick()
        composeTestRule.waitForIdle()

        assertTrue(actions.contains(SignupAction.EnableAutofocus))
    }

    @Test
    fun `next with a valid credential sends SubmitInfo`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, isCredentialValid = true))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf<SignupAction>(SignupAction.SubmitInfo), actions)
    }

    @Test
    fun `next with an invalid phone shows the phone error`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, isCredentialValid = false))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.phone_validation_error)).assertExists()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `next with an invalid email shows the email error`() {
        setCredentialEntry(
            SignUpState(
                showPrivacyDialog = false,
                isCredentialValid = false,
                credential = Credential.Email(),
            ),
        )

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.email_validation_error)).assertExists()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `back sends NavigateBack`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false))

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf<SignupAction>(SignupAction.NavigateBack), actions)
    }

    @Test
    fun `ime go with a valid phone sends SubmitInfo`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, isCredentialValid = true))

        textField().performImeAction()

        assertTrue(actions.contains(SignupAction.SubmitInfo))
    }

    @Test
    fun `ime go with an invalid phone shows the phone error`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, isCredentialValid = false))

        textField().performImeAction()

        composeTestRule.onNodeWithText(string(R.string.phone_validation_error)).assertExists()
        assertTrue(actions.none { it is SignupAction.SubmitInfo })
    }

    @Test
    fun `ime go with an invalid email shows the email error`() {
        setCredentialEntry(
            SignUpState(
                showPrivacyDialog = false,
                isCredentialValid = false,
                credential = Credential.Email(),
            ),
        )

        textField().performImeAction()

        composeTestRule.onNodeWithText(string(R.string.email_validation_error)).assertExists()
        assertTrue(actions.none { it is SignupAction.SubmitInfo })
    }

    @Test
    fun `live web map link is shown only with internet`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, isInternetAvailable = true))

        composeTestRule.onNodeWithText(string(R.string.viewLiveWebMap)).assertIsDisplayed()
    }

    @Test
    fun `live web map link is hidden without internet`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, isInternetAvailable = false))

        composeTestRule.onNodeWithText(string(R.string.viewLiveWebMap)).assertDoesNotExist()
    }

    @Test
    fun `existing user dialog shows the matching user`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, existingUser = existingUser))

        composeTestRule.onNodeWithText(string(R.string.user_exists_header)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.user_exists_message)).assertExists()
        composeTestRule.onNodeWithText("Jane Doe").assertExists()
    }

    @Test
    fun `existing user dialog is hidden without an existing user`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false))

        composeTestRule.onNodeWithText(string(R.string.user_exists_header)).assertDoesNotExist()
    }

    @Test
    fun `selecting the existing user sends ExistingUserSelected`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, existingUser = existingUser))

        composeTestRule.onNodeWithText("Jane Doe").performClick()

        assertEquals(listOf<SignupAction>(SignupAction.ExistingUserSelected(existingUser)), actions)
    }

    @Test
    fun `declining the existing user dialog sends CloseExistingUserDialog`() {
        setCredentialEntry(SignUpState(showPrivacyDialog = false, existingUser = existingUser))

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        assertEquals(listOf<SignupAction>(SignupAction.CloseExistingUserDialog), actions)
    }
}