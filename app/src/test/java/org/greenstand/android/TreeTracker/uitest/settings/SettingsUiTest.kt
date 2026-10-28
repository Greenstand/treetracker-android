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
package org.greenstand.android.TreeTracker.uitest.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.settings.Settings
import org.greenstand.android.TreeTracker.settings.SettingsAction
import org.greenstand.android.TreeTracker.settings.SettingsState
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class SettingsUiTest : ComposeUiTest() {
    private val actions = mutableListOf<SettingsAction>()

    private val powerUser =
        User(
            id = 1L,
            wallet = "power@user.org",
            numberOfTrees = 10,
            firstName = "Power",
            lastName = "User",
            photoPath = "",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    private fun setSettings(state: SettingsState = SettingsState()) {
        setContent {
            Settings(state = state, onHandleAction = { actions += it })
        }
    }

    private fun clickItem(titleRes: Int) {
        composeTestRule.onNodeWithText(string(titleRes)).performClick()
    }

    @Test
    fun `shows the title and the always available items`() {
        setSettings()

        composeTestRule.onNodeWithText(string(R.string.settings)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.profile_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.profile_description)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.edit_trees_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.edit_trees_description)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.privacy_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.privacy_description)).assertIsDisplayed()
    }

    @Test
    fun `shows the app version`() {
        setSettings()

        composeTestRule
            .onNodeWithText(string(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            .assertIsDisplayed()
    }

    @Test
    fun `map item follows the debug or beta flag`() {
        setSettings()

        val mapItem = composeTestRule.onNodeWithText(string(R.string.map_title))
        if (FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA) mapItem.assertIsDisplayed() else mapItem.assertDoesNotExist()
    }

    @Test
    fun `logout and delete account items follow the debug flag`() {
        setSettings()

        val logoutItem = composeTestRule.onNodeWithText(string(R.string.logout_title))
        val deleteItem = composeTestRule.onNodeWithText(string(R.string.delete_account_title))
        if (FeatureFlags.DEBUG_ENABLED) {
            logoutItem.assertIsDisplayed()
            deleteItem.assertIsDisplayed()
        } else {
            logoutItem.assertDoesNotExist()
            deleteItem.assertDoesNotExist()
        }
    }

    @Test
    fun `profile item sends NavigateToProfile`() {
        setSettings()

        clickItem(R.string.profile_title)

        assertEquals(listOf<SettingsAction>(SettingsAction.NavigateToProfile), actions)
    }

    @Test
    fun `edit trees item sends NavigateToEditTrees`() {
        setSettings()

        clickItem(R.string.edit_trees_title)

        assertEquals(listOf<SettingsAction>(SettingsAction.NavigateToEditTrees), actions)
    }

    @Test
    fun `privacy item asks to show the privacy dialog`() {
        setSettings()

        clickItem(R.string.privacy_title)

        assertEquals(listOf<SettingsAction>(SettingsAction.SetPrivacyDialogVisibility(true)), actions)
    }

    @Test
    fun `map item sends NavigateToMap`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA)
        setSettings()

        clickItem(R.string.map_title)

        assertEquals(listOf<SettingsAction>(SettingsAction.NavigateToMap), actions)
    }

    @Test
    fun `logout item asks to show the logout dialog`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        setSettings()

        clickItem(R.string.logout_title)

        assertEquals(listOf<SettingsAction>(SettingsAction.UpdateLogoutDialogVisibility(true)), actions)
    }

    @Test
    fun `delete account item sends NavigateToDeleteAccount`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        setSettings()

        clickItem(R.string.delete_account_title)

        assertEquals(listOf<SettingsAction>(SettingsAction.NavigateToDeleteAccount), actions)
    }

    @Test
    fun `back sends NavigateBack`() {
        setSettings()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.NavigateBack), actions)
    }

    @Test
    fun `privacy dialog is hidden by default`() {
        setSettings()

        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertDoesNotExist()
    }

    @Test
    fun `privacy dialog is shown from state and closes on approve`() {
        setSettings(SettingsState(showPrivacyPolicyDialog = true))
        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertIsDisplayed()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.SetPrivacyDialogVisibility(false)), actions)
    }

    @Test
    fun `logout dialog is hidden by default`() {
        setSettings()

        composeTestRule.onNodeWithText(string(R.string.logout_dialog_title)).assertDoesNotExist()
    }

    @Test
    fun `logout dialog shows the message and the power user`() {
        setSettings(SettingsState(showLogoutDialog = true, powerUser = powerUser))

        composeTestRule.onNodeWithText(string(R.string.logout_dialog_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.logout_dialog_message)).assertExists()
        composeTestRule.onNodeWithText("Power User").assertExists()
    }

    @Test
    fun `confirming the logout dialog sends LogoutConfirmed`() {
        setSettings(SettingsState(showLogoutDialog = true, powerUser = powerUser))

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.LogoutConfirmed), actions)
    }

    @Test
    fun `declining the logout dialog hides it`() {
        setSettings(SettingsState(showLogoutDialog = true, powerUser = powerUser))

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.UpdateLogoutDialogVisibility(false)), actions)
    }
}