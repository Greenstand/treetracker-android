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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.OrgRoute
import org.greenstand.android.TreeTracker.orgpicker.OrgPicker
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerAction
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerState
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test

class OrgPickerUiTest : ComposeUiTest() {
    private val actions = mutableListOf<OrgPickerAction>()

    private val greenstand = org(id = "1", name = "Greenstand")
    private val acme = org(id = "2", name = "Acme Forests")

    private fun org(
        id: String,
        name: String,
    ) = Org(
        id = id,
        name = name,
        walletId = "wallet-$id",
        logoPath = "",
        captureSetupFlow = emptyList(),
        captureFlow = emptyList(),
    )

    private fun setOrgPicker(state: OrgPickerState) {
        setContent {
            OrgPicker(state = state, onHandleAction = { actions += it })
        }
    }

    @Test
    fun `shows the title`() {
        setOrgPicker(OrgPickerState())

        composeTestRule.onNodeWithText(string(R.string.select_organization).uppercase()).assertIsDisplayed()
    }

    @Test
    fun `shows every org in upper case`() {
        setOrgPicker(OrgPickerState(orgs = listOf(greenstand, acme), currentOrg = greenstand))

        composeTestRule.onNodeWithText("GREENSTAND").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACME FORESTS").assertIsDisplayed()
    }

    @Test
    fun `clicking an org sends SetOrg`() {
        setOrgPicker(OrgPickerState(orgs = listOf(greenstand, acme), currentOrg = greenstand))

        composeTestRule.onNodeWithText("ACME FORESTS").performClick()

        assertEquals(listOf<OrgPickerAction>(OrgPickerAction.SetOrg(acme)), actions)
    }

    @Test
    fun `next sends NavigateNext`() {
        setOrgPicker(OrgPickerState(orgs = listOf(greenstand), currentOrg = greenstand))

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        assertEquals(listOf<OrgPickerAction>(OrgPickerAction.NavigateNext), actions)
    }

    private fun launchRoot(): OrgRepo {
        val orgRepo = mockk<OrgRepo>(relaxed = true)
        coEvery { orgRepo.getOrgs() } returns listOf(greenstand, acme)
        every { orgRepo.currentOrg() } returns greenstand
        backStack.add(OrgRoute)
        val viewModel = OrgPickerViewModel(orgRepo)
        setContent(inspectionMode = false) { OrgPickerScreen(viewModel = viewModel) }
        return orgRepo
    }

    @Test
    fun `root shows the orgs from the repository`() {
        launchRoot()

        composeTestRule.onNodeWithText("GREENSTAND").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACME FORESTS").assertIsDisplayed()
    }

    @Test
    fun `root clicking an org makes it the current org`() {
        val orgRepo = launchRoot()

        composeTestRule.onNodeWithText("ACME FORESTS").performClick()
        composeTestRule.waitForIdle()

        coVerify { orgRepo.setOrg(acme.id) }
    }

    @Test
    fun `root next pops the org picker`() {
        launchRoot()

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }
}