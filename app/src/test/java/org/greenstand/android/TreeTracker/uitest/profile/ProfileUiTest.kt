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
package org.greenstand.android.TreeTracker.uitest.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.profile.Profile
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.userselect.UserSelectAction
import org.greenstand.android.TreeTracker.userselect.UserSelectState
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUiTest : ComposeUiTest() {
    private val actions = mutableListOf<UserSelectAction>()

    private val phoneUser =
        User(
            id = 3L,
            wallet = "0712345678",
            numberOfTrees = 12,
            firstName = "Jane",
            lastName = "Doe",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false,
        )

    private val emailUser = phoneUser.copy(wallet = "jane@example.org")

    private fun setProfile(state: UserSelectState) {
        setContent {
            Profile(state = state, onHandleAction = { actions += it })
        }
    }

    private fun textFields() = composeTestRule.onAllNodes(hasSetTextAction())

    private fun photo() = composeTestRule.onNode(hasClickAction() and !hasSetTextAction())

    @Test
    fun `shows a loading message until a user is selected`() {
        setProfile(UserSelectState(selectedUser = null))

        composeTestRule.onNodeWithText(string(R.string.loading_user_profile)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).assertDoesNotExist()
    }

    @Test
    fun `shows the selected user read only`() {
        setProfile(UserSelectState(selectedUser = phoneUser))

        composeTestRule.onNodeWithText(string(R.string.profile_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.first_name_hint)).assertExists()
        composeTestRule.onNodeWithText("Jane").assertExists()
        composeTestRule.onNodeWithText(string(R.string.last_name_hint)).assertExists()
        composeTestRule.onNodeWithText("Doe").assertExists()
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).assertIsDisplayed()
        textFields().assertCountEquals(0)
        composeTestRule.onNodeWithText(string(R.string.save_changes)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.loading_user_profile)).assertDoesNotExist()
    }

    @Test
    fun `phone wallet is labelled as phone`() {
        setProfile(UserSelectState(selectedUser = phoneUser))

        composeTestRule.onNodeWithText(string(R.string.phone_placeholder)).assertExists()
        composeTestRule.onNodeWithText("0712345678").assertExists()
        composeTestRule.onNodeWithText(string(R.string.email_placeholder)).assertDoesNotExist()
    }

    @Test
    fun `email wallet is labelled as email`() {
        setProfile(UserSelectState(selectedUser = emailUser))

        composeTestRule.onNodeWithText(string(R.string.email_placeholder)).assertExists()
        composeTestRule.onNodeWithText("jane@example.org").assertExists()
        composeTestRule.onNodeWithText(string(R.string.phone_placeholder)).assertDoesNotExist()
    }

    @Test
    fun `edit profile sends ToggleEditMode`() {
        setProfile(UserSelectState(selectedUser = phoneUser))

        composeTestRule.onNodeWithText(string(R.string.edit_profile)).performClick()

        assertEquals(listOf<UserSelectAction>(UserSelectAction.ToggleEditMode), actions)
    }

    @Test
    fun `edit mode shows editable fields and the save button`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        composeTestRule.onNodeWithText(string(R.string.cancel_edit)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.edit_profile)).assertDoesNotExist()
        textFields().assertCountEquals(3)
        composeTestRule.onNodeWithText(string(R.string.save_changes)).assertExists()
    }

    @Test
    fun `cancel edit sends ToggleEditMode`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        composeTestRule.onNodeWithText(string(R.string.cancel_edit)).performClick()

        assertEquals(listOf<UserSelectAction>(UserSelectAction.ToggleEditMode), actions)
    }

    @Test
    fun `save with valid names sends SaveUserToDatabase`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        composeTestRule.onNodeWithText(string(R.string.save_changes)).performScrollTo().performClick()

        assertEquals(listOf<UserSelectAction>(UserSelectAction.SaveUserToDatabase), actions)
    }

    @Test
    fun `save with an invalid name does nothing`() {
        setProfile(UserSelectState(selectedUser = phoneUser.copy(firstName = "J"), editMode = true))

        composeTestRule.onNodeWithText(string(R.string.save_changes)).performScrollTo().performClick()

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `editing the first name sends the filtered name`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        textFields()[0].performTextReplacement("Ann3")

        assertEquals(listOf<UserSelectAction>(UserSelectAction.UpdateSelectedUser(firstName = "Ann")), actions)
    }

    @Test
    fun `editing the last name sends the filtered name`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        textFields()[1].performTextReplacement("Lee!")

        assertEquals(listOf<UserSelectAction>(UserSelectAction.UpdateSelectedUser(lastName = "Lee")), actions)
    }

    @Test
    fun `a too short first name shows a validation error`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        textFields()[0].performTextReplacement("A")

        composeTestRule.onNodeWithText(NAME_TOO_SHORT_ERROR).assertExists()
    }

    @Test
    fun `a too short last name shows a validation error`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        textFields()[1].performTextReplacement("B")

        composeTestRule.onNodeWithText(NAME_TOO_SHORT_ERROR).assertExists()
    }

    @Test
    fun `a valid first name shows no validation error`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        textFields()[0].performTextReplacement("Ann")

        composeTestRule.onNodeWithText(NAME_TOO_SHORT_ERROR).assertDoesNotExist()
    }

    @Test
    fun `editing a phone wallet sends the new phone`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        textFields()[2].performTextReplacement("0799999999")

        assertEquals(listOf<UserSelectAction>(UserSelectAction.UpdateSelectedUser(phone = "0799999999")), actions)
    }

    @Test
    fun `editing an email wallet sends the new email`() {
        setProfile(UserSelectState(selectedUser = emailUser, editMode = true))

        textFields()[2].performTextReplacement("ann@example.org")

        assertEquals(listOf<UserSelectAction>(UserSelectAction.UpdateSelectedUser(email = "ann@example.org")), actions)
    }

    @Test
    fun `photo can be retaken in edit mode`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = true))

        photo().assertIsEnabled()
        photo().performClick()

        assertEquals(listOf<UserSelectAction>(UserSelectAction.NavigateToPhoto), actions)
    }

    @Test
    fun `photo is not clickable outside edit mode`() {
        setProfile(UserSelectState(selectedUser = phoneUser, editMode = false))

        photo().assertIsNotEnabled()
    }

    @Test
    fun `back sends NavigateBack`() {
        setProfile(UserSelectState(selectedUser = phoneUser))

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf<UserSelectAction>(UserSelectAction.NavigateBack), actions)
    }

    private companion object {
        const val NAME_TOO_SHORT_ERROR = "Name must be at least 2 characters"
    }
}