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
package org.greenstand.android.TreeTracker.userselect

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class UserSelectViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    @MockK(relaxed = true)
    private lateinit var messagesRepo: MessagesRepo

    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    private lateinit var captureSetupData: CaptureSetupData

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.open() } just Runs
        captureSetupData = mockk(relaxed = true)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { userRepo.users() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    private fun createViewModel(userId: Long? = null): UserSelectViewModel {
        return UserSelectViewModel(
            userId = userId,
            userRepo = userRepo,
            messageRepo = messagesRepo,
            locationDataCapturer = locationDataCapturer,
            prefs = preferences,
        )
    }

    @Test
    fun `WHEN selectUser called THEN sets prefs userId and updates state selectedUser`() = runTest {
        val viewModel = createViewModel()
        val user = FakeFileGenerator.fakeUsers.first()

        viewModel.handleAction(UserSelectAction.SelectUser(user))

        verify { preferences.setUserId(user.id) }
        verify { captureSetupData.user = user }
        val state = viewModel.state.first()
        assertNotNull(state.selectedUser)
        assertEquals(user.id, state.selectedUser!!.id)
    }

    @Test
    fun `WHEN updateEditEnabled called THEN toggles editMode in state`() = runTest {
        val viewModel = createViewModel()

        val initialState = viewModel.state.first()
        assertFalse(initialState.editMode)

        viewModel.handleAction(UserSelectAction.ToggleEditMode)

        val updatedState = viewModel.state.first()
        assertTrue(updatedState.editMode)

        viewModel.handleAction(UserSelectAction.ToggleEditMode)

        val toggledBackState = viewModel.state.first()
        assertFalse(toggledBackState.editMode)
    }

    @Test
    fun `WHEN updateSelectedUser called THEN updates only provided fields`() = runTest {
        val viewModel = createViewModel()
        val user = FakeFileGenerator.fakeUsers.first()
        viewModel.handleAction(UserSelectAction.SelectUser(user))

        viewModel.handleAction(UserSelectAction.UpdateSelectedUser(firstName = "UpdatedFirst"))

        val state = viewModel.state.first()
        assertNotNull(state.selectedUser)
        assertEquals("UpdatedFirst", state.selectedUser!!.firstName)
        assertEquals(user.lastName, state.selectedUser!!.lastName)
        assertEquals(user.photoPath, state.selectedUser!!.photoPath)
    }

    @Test
    fun `WHEN updateDeleteProfileState called THEN updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.handleAction(UserSelectAction.UpdateDeleteProfileState(DeleteProfileState.SHOWDIALOG))

        val state = viewModel.state.first()
        assertEquals(DeleteProfileState.SHOWDIALOG, state.deleteProfileState)
    }
}
