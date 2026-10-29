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

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DeleteProfileRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.profile.DeleteProfileScreen
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeleteProfileScreenUiTest : UserSelectRootUiTest() {
    @Before
    fun setUp() {
        backStack.add(SettingsRoute)
        backStack.add(DeleteProfileRoute)
        coEvery { userRepo.deleteUser(any()) } returns true
    }

    private fun launchDeleteProfile(user: User = planter) {
        every { userRepo.users() } returns flowOf(listOf(user))
        startKoinWith(userSelectViewModelModule())
        setRootContent { DeleteProfileScreen() }
    }

    private fun requestDeletion(user: User = planter) {
        composeTestRule.onNodeWithText("${user.firstName} ${user.lastName}").performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.delete_account_dialog_message)).assertExists()
    }

    @Test
    fun `no dialog is shown before a planter is chosen`() {
        launchDeleteProfile()

        composeTestRule.onNodeWithText(string(R.string.delete_account_dialog_message)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.account_deleted)).assertDoesNotExist()
    }

    @Test
    fun `choosing a planter asks to confirm the deletion`() {
        launchDeleteProfile()

        requestDeletion()

        composeTestRule.onNodeWithText(string(R.string.delete_account_title)).assertExists()
        coVerify(exactly = 0) { userRepo.deleteUser(any()) }
    }

    @Test
    fun `declining the deletion closes the dialog without deleting`() {
        launchDeleteProfile()
        requestDeletion()

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.delete_account_dialog_message)).assertDoesNotExist()
        coVerify(exactly = 0) { userRepo.deleteUser(any()) }
        assertEquals(listOf(DashboardRoute, SettingsRoute, DeleteProfileRoute), backStack.toList())
    }

    @Test
    fun `confirming the deletion deletes the planter and shows the deleted dialog`() {
        launchDeleteProfile()
        requestDeletion()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.account_deleted)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.account_deleted_message)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.delete_account_dialog_message)).assertDoesNotExist()
        coVerify { userRepo.deleteUser(planter.wallet) }
        coVerify { messagesRepo.saveMessage(planter.wallet, "admin", any()) }
    }

    @Test
    fun `acknowledging the deletion of a planter returns to settings`() {
        launchDeleteProfile()
        requestDeletion()
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.account_deleted)).assertExists()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `acknowledging the deletion of the power user restarts sign up`() {
        val powerUser = planter.copy(isPowerUser = true)
        launchDeleteProfile(powerUser)
        requestDeletion(powerUser)
        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.account_deleted)).assertExists()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(SignupFlowRoute), backStack.toList())
    }
}