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

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DeleteProfileRoute
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.ProfileSelectRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.settings.SettingsScreen
import org.greenstand.android.TreeTracker.settings.SettingsViewModel
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.TestTags
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module
import org.maplibre.android.MapLibre

/**
 * Tests for the [SettingsScreen] root: the real [SettingsViewModel] and its navigation events.
 * `MapLibre.getInstance` needs native code Robolectric cannot load, so it is stubbed.
 */
class SettingsScreenUiTest : RootScreenUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)

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

    @Before
    fun stubMapLibre() {
        mockkStatic(MapLibre::class)
        every { MapLibre.getInstance(any()) } returns mockk(relaxed = true)
    }

    @After
    fun unstubMapLibre() {
        unmockkStatic(MapLibre::class)
    }

    private fun launchSettings(currentPowerUser: User? = powerUser) {
        coEvery { userRepo.getPowerUser() } returns currentPowerUser
        val viewModel = SettingsViewModel(userRepo)
        startKoinWith(module { single { viewModel } })
        backStack.add(SettingsRoute)
        setRootContent { SettingsScreen() }
        composeTestRule.waitUntil { viewModel.state.value.powerUser == currentPowerUser }
    }

    private fun clickItem(titleRes: Int) {
        composeTestRule.onNodeWithText(string(titleRes)).performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `profile item opens the profile picker`() {
        launchSettings()

        clickItem(R.string.profile_title)

        assertEquals(listOf(DashboardRoute, SettingsRoute, ProfileSelectRoute), backStack.toList())
    }

    @Test
    fun `edit trees item opens the tree edit user picker`() {
        launchSettings()

        clickItem(R.string.edit_trees_title)

        assertEquals(listOf(DashboardRoute, SettingsRoute, TreeEditUserSelectRoute), backStack.toList())
    }

    @Test
    fun `map item opens the map`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED || FeatureFlags.BETA)
        launchSettings()

        clickItem(R.string.map_title)

        assertEquals(listOf(DashboardRoute, SettingsRoute, MapRoute), backStack.toList())
    }

    @Test
    fun `delete account item opens the delete profile picker`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        launchSettings()

        clickItem(R.string.delete_account_title)

        assertEquals(listOf(DashboardRoute, SettingsRoute, DeleteProfileRoute), backStack.toList())
    }

    @Test
    fun `back pops settings`() {
        launchSettings()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `privacy item opens the privacy policy and approving closes it`() {
        launchSettings()

        clickItem(R.string.privacy_title)
        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertExists()

        composeTestRule.onNodeWithTag(TestTags.PRIVACY_POLICY_CLOSE_BUTTON).performClick()

        composeTestRule.onNodeWithText(string(R.string.privacy_policy)).assertDoesNotExist()
        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `logout item shows the power user loaded from the repository`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        launchSettings()

        clickItem(R.string.logout_title)

        composeTestRule.onNodeWithText(string(R.string.logout_dialog_message)).assertExists()
        composeTestRule.onNodeWithText("Power User").assertExists()
    }

    @Test
    fun `logout dialog without a power user shows no user card`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        launchSettings(currentPowerUser = null)

        clickItem(R.string.logout_title)

        composeTestRule.onNodeWithText(string(R.string.logout_dialog_message)).assertExists()
        composeTestRule.onNodeWithText("Power User").assertDoesNotExist()
    }

    @Test
    fun `declining the logout keeps the power user and stays on settings`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        launchSettings()
        clickItem(R.string.logout_title)

        composeTestRule.onNodeWithTag(TestTags.DIALOG_NEGATIVE_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.logout_dialog_message)).assertDoesNotExist()
        coVerify(exactly = 0) { userRepo.setPowerUserStatus(any(), any()) }
        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `confirming the logout drops the power user and restarts sign up`() {
        assumeTrue(FeatureFlags.DEBUG_ENABLED)
        launchSettings()
        clickItem(R.string.logout_title)

        composeTestRule.onNodeWithTag(TestTags.DIALOG_POSITIVE_BUTTON).performClick()
        composeTestRule.waitForIdle()

        coVerify { userRepo.setPowerUserStatus(powerUser.id, false) }
        assertEquals(listOf(SignupFlowRoute), backStack.toList())
    }
}