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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
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
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.orgpicker.AddOrg
import org.greenstand.android.TreeTracker.orgpicker.AddOrgAction
import org.greenstand.android.TreeTracker.orgpicker.AddOrgScreen
import org.greenstand.android.TreeTracker.orgpicker.AddOrgState
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddOrgUiTest : ComposeUiTest() {
    private val actions = mutableListOf<AddOrgAction>()

    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val captureSetupNav = mockk<CaptureSetupNavigationController>(relaxed = true)

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun setAddOrg(state: AddOrgState) {
        setContent {
            AddOrg(state = state, onHandleAction = { actions += it })
        }
    }

    private fun launchRoot() {
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
        val orgRepo = mockk<OrgRepo>(relaxed = true)
        every { orgRepo.currentOrg() } returns
            Org(
                id = "1",
                name = "Greenstand",
                walletId = "",
                logoPath = "",
                captureSetupFlow = emptyList(),
                captureFlow = emptyList(),
            )
        val viewModel = AddOrgViewModel(mockk<Preferences>(relaxed = true), orgRepo)
        setContent(inspectionMode = false) { AddOrgScreen(viewModel = viewModel) }
    }

    @Test
    fun `shows an empty org field with a hint`() {
        setAddOrg(AddOrgState())

        composeTestRule.onNodeWithText(string(R.string.organization)).assertIsDisplayed()
    }

    @Test
    fun `shows the org name from state`() {
        setAddOrg(AddOrgState(orgName = "Acme Forests"))

        composeTestRule.onNode(hasSetTextAction()).assertExists()
        composeTestRule.onNodeWithText("Acme Forests").assertIsDisplayed()
    }

    @Test
    fun `typing an org name sends UpdateOrgName`() {
        setAddOrg(AddOrgState())

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Acme")

        assertEquals(listOf<AddOrgAction>(AddOrgAction.UpdateOrgName("Acme")), actions)
    }

    @Test
    fun `previous org shortcut is hidden without a previous org`() {
        setAddOrg(AddOrgState(previousOrgName = null))

        composeTestRule.onNodeWithText("Old Org").assertDoesNotExist()
    }

    @Test
    fun `previous org shortcut applies the autofill`() {
        setAddOrg(AddOrgState(previousOrgName = "Old Org"))

        composeTestRule.onNodeWithText("Old Org").performClick()

        assertEquals(listOf<AddOrgAction>(AddOrgAction.ApplyOrgAutofill), actions)
    }

    @Test
    fun `back sends NavigateBack`() {
        setAddOrg(AddOrgState())

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf<AddOrgAction>(AddOrgAction.NavigateBack), actions)
    }

    @Test
    fun `next sends NavigateNext`() {
        setAddOrg(AddOrgState())

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf<AddOrgAction>(AddOrgAction.NavigateNext), actions)
    }

    @Test
    fun `ime go sends NavigateNext`() {
        setAddOrg(AddOrgState(orgName = "Acme"))

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        assertTrue(actions.contains(AddOrgAction.NavigateNext))
    }

    @Test
    fun `root back steps back in the capture setup flow`() {
        launchRoot()

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { captureSetupNav.navBackward(navigator) }
    }

    @Test
    fun `root next saves the org name and moves forward in the capture setup flow`() {
        launchRoot()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Acme")

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        verify { captureSetupData.organizationName = "Acme" }
        coVerify { captureSetupNav.navForward(navigator) }
    }
}