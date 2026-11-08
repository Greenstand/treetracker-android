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
package org.greenstand.android.TreeTracker.orgpicker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AddOrgViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo

    @MockK(relaxed = true)
    private lateinit var mockEditor: Preferences.Editor

    private lateinit var captureSetupData: CaptureSetupData

    private val defaultOrg =
        Org(
            id = "-1",
            name = "Greenstand",
            walletId = "",
            logoPath = "",
            captureSetupFlow = emptyList(),
            captureFlow = emptyList(),
        )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(CaptureSetupScopeManager)
        captureSetupData = mockk(relaxed = true)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { preferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { orgRepo.currentOrg() } returns defaultOrg
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `WHEN init THEN loads userImagePath and previousOrgName from prefs`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns "PreviousOrg"

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            val state = viewModel.state.value
            assertEquals(fakeUser.photoPath, state.userImagePath)
            assertEquals("PreviousOrg", state.previousOrgName)
        }

    @Test
    fun `WHEN updateOrgName called THEN updates state and CaptureSetupScopeManager`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns null

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            viewModel.handleAction(AddOrgAction.UpdateOrgName("NewOrg"))

            val state = viewModel.state.value
            assertEquals("NewOrg", state.orgName)
            verify { captureSetupData.organizationName = "NewOrg" }
        }

    @Test
    fun `WHEN applyOrgAutofill called THEN sets orgName to previousOrgName`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns "AutofillOrg"

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            viewModel.handleAction(AddOrgAction.ApplyOrgAutofill)

            val state = viewModel.state.value
            assertEquals("AutofillOrg", state.orgName)
        }

    @Test
    fun `WHEN setDefaultOrg called with non-blank orgName THEN saves org name to prefs`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns null

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            viewModel.handleAction(AddOrgAction.UpdateOrgName("SavedOrg"))
            viewModel.handleAction(AddOrgAction.SetDefaultOrg)

            verify { mockEditor.putString(any(), "SavedOrg") }
            verify { mockEditor.apply() }
        }

    // --- Org pre-fill tests ---

    @Test
    fun `WHEN current org is non-default THEN orgName is pre-filled`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns null
            every { orgRepo.currentOrg() } returns defaultOrg.copy(id = "123", name = "Kasiki Hai")

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            assertEquals("Kasiki Hai", viewModel.state.value.orgName)
            coVerify { captureSetupData.organizationName = "Kasiki Hai" }
        }

    @Test
    fun `WHEN current org is Greenstand default THEN orgName is not pre-filled`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns null

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            assertEquals("", viewModel.state.value.orgName)
            coVerify(exactly = 0) { captureSetupData.organizationName = "Greenstand" }
        }

    @Test
    fun `WHEN current org has blank name THEN orgName is not pre-filled`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns null
            every { orgRepo.currentOrg() } returns defaultOrg.copy(name = "")

            val viewModel = AddOrgViewModel(preferences, orgRepo)

            assertEquals("", viewModel.state.value.orgName)
        }

    @Test
    fun `WHEN org is pre-filled and user types over it THEN CaptureSetupData gets user value`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            every { preferences.getString(any(), any()) } returns null
            every { orgRepo.currentOrg() } returns defaultOrg.copy(id = "123", name = "Kasiki Hai")

            val viewModel = AddOrgViewModel(preferences, orgRepo)
            viewModel.handleAction(AddOrgAction.UpdateOrgName("Different Org"))

            assertEquals("Different Org", viewModel.state.value.orgName)
            verify { captureSetupData.organizationName = "Different Org" }
        }

    private fun createViewModel(previousOrg: String? = null): AddOrgViewModel {
        every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
        every { preferences.getString(any(), any()) } returns previousOrg
        return AddOrgViewModel(preferences, orgRepo)
    }

    @Test
    fun `GIVEN no saved org WHEN created THEN previous org name is null and no events are emitted`() =
        runTest {
            val viewModel = createViewModel(previousOrg = null)

            assertNull(viewModel.state.value.previousOrgName)
            assertEquals("", viewModel.state.value.orgName)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN created THEN previous org name is read from the autofill org key`() =
        runTest {
            createViewModel(previousOrg = "PreviousOrg")

            verify { preferences.getString(match { it.path.endsWith("/autofill-org") }, null) }
        }

    @Test
    fun `GIVEN current org name is whitespace WHEN created THEN org name is not pre-filled`() =
        runTest {
            every { orgRepo.currentOrg() } returns defaultOrg.copy(id = "9", name = "   ")

            val viewModel = createViewModel()

            assertEquals("", viewModel.state.value.orgName)
            verify(exactly = 0) { captureSetupData.organizationName = any() }
        }

    @Test
    fun `WHEN autofill is applied THEN org name becomes the previous org without touching capture data`() =
        runTest {
            val viewModel = createViewModel(previousOrg = "AutofillOrg")

            viewModel.handleAction(AddOrgAction.ApplyOrgAutofill)

            assertEquals("AutofillOrg", viewModel.state.value.orgName)
            verify(exactly = 0) { captureSetupData.organizationName = any() }
        }

    @Test
    fun `GIVEN autofill applied WHEN default org is set THEN autofilled name is saved and used`() =
        runTest {
            val viewModel = createViewModel(previousOrg = "AutofillOrg")
            viewModel.handleAction(AddOrgAction.ApplyOrgAutofill)

            viewModel.handleAction(AddOrgAction.SetDefaultOrg)

            verify { mockEditor.putString(match { it.path.endsWith("/autofill-org") }, "AutofillOrg") }
            verify { mockEditor.apply() }
            verify { captureSetupData.organizationName = "AutofillOrg" }
        }

    @Test
    fun `GIVEN pre-filled org WHEN default org is set without typing THEN pre-filled name is saved`() =
        runTest {
            every { orgRepo.currentOrg() } returns defaultOrg.copy(id = "123", name = "Kasiki Hai")
            val viewModel = createViewModel()

            viewModel.handleAction(AddOrgAction.SetDefaultOrg)

            verify { mockEditor.putString(any(), "Kasiki Hai") }
            verify(exactly = 2) { captureSetupData.organizationName = "Kasiki Hai" }
        }

    @Test
    fun `GIVEN empty org name WHEN default org is set THEN nothing is saved but capture data gets the empty name`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleAction(AddOrgAction.SetDefaultOrg)

            verify(exactly = 0) { preferences.edit() }
            verify(exactly = 0) { mockEditor.putString(any(), any()) }
            verify { captureSetupData.organizationName = "" }
        }

    @Test
    fun `GIVEN whitespace org name WHEN default org is set THEN nothing is saved`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(AddOrgAction.UpdateOrgName("   "))

            viewModel.handleAction(AddOrgAction.SetDefaultOrg)

            verify(exactly = 0) { mockEditor.putString(any(), any()) }
            verify(exactly = 2) { captureSetupData.organizationName = "   " }
        }

    @Test
    fun `WHEN org name is updated THEN user image and previous org are kept`() =
        runTest {
            val viewModel = createViewModel(previousOrg = "PreviousOrg")

            viewModel.handleAction(AddOrgAction.UpdateOrgName("Typed"))

            val state = viewModel.state.value
            assertEquals("Typed", state.orgName)
            assertEquals("PreviousOrg", state.previousOrgName)
            assertEquals(FakeFileGenerator.fakeUsers.first().photoPath, state.userImagePath)
        }

    @Test
    fun `WHEN navigation actions are sent THEN state is unchanged and no events are emitted`() =
        runTest {
            val viewModel = createViewModel(previousOrg = "PreviousOrg")
            viewModel.handleAction(AddOrgAction.UpdateOrgName("Typed"))
            val before = viewModel.state.value

            viewModel.handleAction(AddOrgAction.NavigateBack)
            viewModel.handleAction(AddOrgAction.NavigateNext)

            assertEquals(before, viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
            verify(exactly = 0) { mockEditor.putString(any(), any()) }
        }
}