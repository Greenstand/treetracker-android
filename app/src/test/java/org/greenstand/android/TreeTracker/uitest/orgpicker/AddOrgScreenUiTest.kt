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
package org.greenstand.android.TreeTracker.uitest.orgpicker

import android.content.Context
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.orgpicker.AddOrgScreen
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * Tests for the [AddOrgScreen] root with a real [AddOrgViewModel] over real [Preferences],
 * covering the current-org prefill and the "previous org" autofill shortcut.
 */
class AddOrgScreenUiTest : ComposeUiTest() {
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val captureSetupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private val preferences =
        Preferences(
            RuntimeEnvironment.getApplication().getSharedPreferences("add-org-ui-test", Context.MODE_PRIVATE),
        )

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.nav } returns captureSetupNav
        every { captureSetupData.user } returns
            User(
                id = 1L,
                wallet = "0712345678",
                numberOfTrees = 0,
                firstName = "Jane",
                lastName = "Doe",
                photoPath = "",
                isPowerUser = true,
                unreadMessagesAvailable = false,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun launch(
        currentOrgName: String = "Greenstand",
        previousOrgName: String? = null,
    ) {
        every { orgRepo.currentOrg() } returns
            Org(
                id = "1",
                name = currentOrgName,
                walletId = "",
                logoPath = "",
                captureSetupFlow = emptyList(),
                captureFlow = emptyList(),
            )
        previousOrgName?.let { preferences.edit().putString(PREV_ORG_KEY, it).apply() }
        val viewModel = AddOrgViewModel(preferences, orgRepo)
        setContent(inspectionMode = false) { AddOrgScreen(viewModel = viewModel) }
    }

    private fun orgField() = composeTestRule.onNode(hasSetTextAction())

    @Test
    fun `a custom current org is prefilled and becomes the setup org`() {
        launch(currentOrgName = "Acme Forests")

        composeTestRule.onNode(hasSetTextAction() and hasText("Acme Forests")).assertExists()
        verify { captureSetupData.organizationName = "Acme Forests" }
    }

    @Test
    fun `the default Greenstand org is not prefilled`() {
        launch(currentOrgName = "Greenstand")

        composeTestRule.onNode(hasSetTextAction() and hasText("Greenstand")).assertDoesNotExist()
        verify(exactly = 0) { captureSetupData.organizationName = "Greenstand" }
    }

    @Test
    fun `a blank current org is not prefilled`() {
        launch(currentOrgName = "  ")

        verify(exactly = 0) { captureSetupData.organizationName = any() }
    }

    @Test
    fun `no previous org means no autofill shortcut`() {
        launch()

        composeTestRule.onNodeWithText("Old Org").assertDoesNotExist()
    }

    @Test
    fun `the previous org shortcut fills in the org field`() {
        launch(previousOrgName = "Old Org")

        composeTestRule.onNodeWithText("Old Org").performClick()

        composeTestRule.onNode(hasSetTextAction() and hasText("Old Org")).assertExists()
    }

    @Test
    fun `next remembers the entered org for the next autofill`() {
        launch()
        orgField().performTextInput("Acme")

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals("Acme", preferences.getString(PREV_ORG_KEY))
        coVerify { captureSetupNav.navForward(navigator) }
    }

    @Test
    fun `next with an empty org keeps the previous autofill`() {
        launch(previousOrgName = "Old Org")

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals("Old Org", preferences.getString(PREV_ORG_KEY))
        verify { captureSetupData.organizationName = "" }
        coVerify { captureSetupNav.navForward(navigator) }
    }

    @Test
    fun `next with no org and no previous org stores nothing`() {
        launch()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertNull(preferences.getString(PREV_ORG_KEY))
    }

    @Test
    fun `the autofilled org is saved as the setup org on next`() {
        launch(previousOrgName = "Old Org")
        composeTestRule.onNodeWithText("Old Org").performClick()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { captureSetupData.organizationName = "Old Org" }
        coVerify { captureSetupNav.navForward(navigator) }
    }

    @Test
    fun `ime go on the org field moves forward with the typed org`() {
        launch()
        orgField().performTextInput("Acme")

        orgField().performImeAction()
        composeTestRule.waitForIdle()

        assertEquals("Acme", preferences.getString(PREV_ORG_KEY))
        coVerify { captureSetupNav.navForward(navigator) }
    }

    private companion object {
        val PREV_ORG_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("autofill-org")
    }
}