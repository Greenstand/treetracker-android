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

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.signup.NameEntryView
import org.greenstand.android.TreeTracker.signup.SignUpState
import org.greenstand.android.TreeTracker.signup.SignupAction
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameEntryUiTest : ComposeUiTest() {
    private val actions = mutableListOf<SignupAction>()

    private fun setNameEntry(
        state: SignUpState = SignUpState(isCredentialView = false),
        isFormValid: Boolean = false,
    ) {
        setContent {
            NameEntryView(
                state = state,
                onHandleAction = { actions += it },
                isFormValid = { isFormValid },
            )
        }
    }

    private fun firstNameField() = composeTestRule.onAllNodes(hasSetTextAction())[0]

    private fun lastNameField() = composeTestRule.onAllNodes(hasSetTextAction())[1]

    @Test
    fun `shows empty first and last name fields with hints`() {
        setNameEntry()

        composeTestRule.onAllNodes(hasSetTextAction()).assertCountEquals(2)
        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.last_name_hint)).assertIsDisplayed()
    }

    @Test
    fun `shows the names from state`() {
        setNameEntry(SignUpState(isCredentialView = false, firstName = "John", lastName = "Doe"))

        composeTestRule.onNodeWithText("John").assertIsDisplayed()
        composeTestRule.onNodeWithText("Doe").assertIsDisplayed()
    }

    @Test
    fun `typing a first name sends UpdateFirstName`() {
        setNameEntry()

        firstNameField().performTextInput("Ann")

        assertEquals(listOf<SignupAction>(SignupAction.UpdateFirstName("Ann")), actions)
    }

    @Test
    fun `typing a last name sends UpdateLastName`() {
        setNameEntry()

        lastNameField().performTextInput("Lee")

        assertEquals(listOf<SignupAction>(SignupAction.UpdateLastName("Lee")), actions)
    }

    @Test
    fun `shows name errors from state`() {
        setNameEntry(
            SignUpState(
                isCredentialView = false,
                firstName = "A",
                lastName = "B",
                firstNameError = "First name error",
                lastNameError = "Last name error",
            ),
        )

        composeTestRule.onNodeWithText("First name error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last name error").assertIsDisplayed()
    }

    @Test
    fun `hides name errors when state has none`() {
        setNameEntry(SignUpState(isCredentialView = false, firstName = "Ann", lastName = "Lee"))

        composeTestRule.onNodeWithText("First name error").assertDoesNotExist()
        composeTestRule.onNodeWithText("Last name error").assertDoesNotExist()
    }

    @Test
    fun `next is disabled while the form is invalid`() {
        setNameEntry(isFormValid = false)

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `next sends LaunchCamera when the form is valid`() {
        setNameEntry(
            state = SignUpState(isCredentialView = false, firstName = "Ann", lastName = "Lee"),
            isFormValid = true,
        )

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf<SignupAction>(SignupAction.LaunchCamera), actions)
    }

    @Test
    fun `back sends GoToCredentialEntry`() {
        setNameEntry()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf<SignupAction>(SignupAction.GoToCredentialEntry), actions)
    }

    @Test
    fun `ime go on last name with both names sends LaunchCamera`() {
        setNameEntry(SignUpState(isCredentialView = false, firstName = "Ann", lastName = "Lee"))

        lastNameField().performImeAction()

        assertEquals(listOf<SignupAction>(SignupAction.LaunchCamera), actions)
    }

    @Test
    fun `ime go on last name without a first name does nothing`() {
        setNameEntry(SignUpState(isCredentialView = false, firstName = null, lastName = "Lee"))

        lastNameField().performImeAction()

        assertTrue(actions.isEmpty())
    }
}