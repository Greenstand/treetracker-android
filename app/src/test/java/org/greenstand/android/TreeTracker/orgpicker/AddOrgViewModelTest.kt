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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AddOrgViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    @MockK(relaxed = true)
    private lateinit var mockEditor: Preferences.Editor

    private lateinit var captureSetupData: CaptureSetupData

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(CaptureSetupScopeManager)
        captureSetupData = mockk(relaxed = true)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { preferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `WHEN init THEN loads userImagePath and previousOrgName from prefs`() = runTest {
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        every { captureSetupData.user } returns fakeUser
        every { preferences.getString(any(), any()) } returns "PreviousOrg"

        val viewModel = AddOrgViewModel(preferences)

        val state = viewModel.state.value
        assertEquals(fakeUser.photoPath, state.userImagePath)
        assertEquals("PreviousOrg", state.previousOrgName)
    }

    @Test
    fun `WHEN updateOrgName called THEN updates state and CaptureSetupScopeManager`() = runTest {
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        every { captureSetupData.user } returns fakeUser
        every { preferences.getString(any(), any()) } returns null

        val viewModel = AddOrgViewModel(preferences)

        viewModel.handleAction(AddOrgAction.UpdateOrgName("NewOrg"))

        val state = viewModel.state.value
        assertEquals("NewOrg", state.orgName)
        verify { captureSetupData.organizationName = "NewOrg" }
    }

    @Test
    fun `WHEN applyOrgAutofill called THEN sets orgName to previousOrgName`() = runTest {
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        every { captureSetupData.user } returns fakeUser
        every { preferences.getString(any(), any()) } returns "AutofillOrg"

        val viewModel = AddOrgViewModel(preferences)

        viewModel.handleAction(AddOrgAction.ApplyOrgAutofill)

        val state = viewModel.state.value
        assertEquals("AutofillOrg", state.orgName)
    }

    @Test
    fun `WHEN setDefaultOrg called with non-blank orgName THEN saves org name to prefs`() = runTest {
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        every { captureSetupData.user } returns fakeUser
        every { preferences.getString(any(), any()) } returns null

        val viewModel = AddOrgViewModel(preferences)

        viewModel.handleAction(AddOrgAction.UpdateOrgName("SavedOrg"))
        viewModel.handleAction(AddOrgAction.SetDefaultOrg)

        verify { mockEditor.putString(any(), "SavedOrg") }
        verify { mockEditor.apply() }
    }
}
