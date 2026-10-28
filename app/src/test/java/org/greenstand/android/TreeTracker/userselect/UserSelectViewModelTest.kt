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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
import kotlin.test.assertNull
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

    private fun createViewModel(userId: Long? = null): UserSelectViewModel =
        UserSelectViewModel(
            userId = userId,
            userRepo = userRepo,
            messageRepo = messagesRepo,
            locationDataCapturer = locationDataCapturer,
            prefs = preferences,
        )

    @Test
    fun `WHEN selectUser called THEN sets prefs userId and updates state selectedUser`() =
        runTest {
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
    fun `WHEN updateEditEnabled called THEN toggles editMode in state`() =
        runTest {
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
    fun `WHEN updateSelectedUser called THEN updates only provided fields`() =
        runTest {
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
    fun `WHEN updateDeleteProfileState called THEN updates state`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleAction(UserSelectAction.UpdateDeleteProfileState(DeleteProfileState.SHOWDIALOG))

            val state = viewModel.state.first()
            assertEquals(DeleteProfileState.SHOWDIALOG, state.deleteProfileState)
        }

    private val planter = FakeFileGenerator.fakeUsers.first().copy(wallet = "0712345678")

    // Init

    @Test
    fun `WHEN created THEN opens the capture setup scope and starts GPS updates`() =
        runTest {
            createViewModel()

            verify { CaptureSetupScopeManager.open() }
            verify { locationDataCapturer.startGpsUpdates() }
        }

    @Test
    fun `WHEN created without users THEN state is the default and no events are emitted`() =
        runTest {
            val viewModel = createViewModel()

            assertEquals(UserSelectState(), viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN the users flow emits THEN state users follow it`() =
        runTest {
            val users = MutableStateFlow(FakeFileGenerator.fakeUsers.take(1))
            every { userRepo.users() } returns users
            val viewModel = createViewModel()
            assertEquals(FakeFileGenerator.fakeUsers.take(1), viewModel.state.value.users)

            users.value = FakeFileGenerator.fakeUsers

            assertEquals(FakeFileGenerator.fakeUsers, viewModel.state.value.users)
        }

    @Test
    fun `GIVEN no userId WHEN created THEN no user is preloaded`() =
        runTest {
            val viewModel = createViewModel(userId = null)

            assertNull(viewModel.state.value.selectedUser)
            coVerify(exactly = 0) { userRepo.getUser(any()) }
        }

    @Test
    fun `GIVEN a userId WHEN created THEN that user is preloaded as selected`() =
        runTest {
            coEvery { userRepo.getUser(planter.id) } returns planter

            val viewModel = createViewModel(userId = planter.id)

            val state =
                withContext(Dispatchers.Default) {
                    withTimeout(5_000) { viewModel.state.first { it.selectedUser != null } }
                }
            assertEquals(planter, state.selectedUser)
        }

    // Select

    @Test
    fun `WHEN another user is selected THEN it replaces the previous selection`() =
        runTest {
            val viewModel = createViewModel()
            val second = FakeFileGenerator.fakeUsers[1]
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.SelectUser(second))

            assertEquals(second, viewModel.state.value.selectedUser)
            verify { preferences.setUserId(second.id) }
            verify { captureSetupData.user = second }
        }

    // Delete

    @Test
    fun `GIVEN a selected user WHEN delete succeeds THEN marks deleted locally and asks admin to delete the account`() =
        runTest {
            coEvery { userRepo.deleteUser("0712345678") } returns true
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.DeleteUser)

            assertEquals(DeleteProfileState.ACCOUNTDELETEDLOCALLY, viewModel.state.value.deleteProfileState)
            coVerifyOrder {
                userRepo.deleteUser("0712345678")
                messagesRepo.saveMessage(
                    wallet = "0712345678",
                    to = "admin",
                    body = "Hi admin, I would like to delete my account with the wallet 0712345678 I understand that all my data will be lost.",
                )
                messagesRepo.syncMessages()
            }
        }

    @Test
    fun `GIVEN a selected user WHEN delete fails THEN state is unchanged and no admin message is sent`() =
        runTest {
            coEvery { userRepo.deleteUser("0712345678") } returns false
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))
            viewModel.handleAction(UserSelectAction.UpdateDeleteProfileState(DeleteProfileState.SHOWDIALOG))

            viewModel.handleAction(UserSelectAction.DeleteUser)

            assertEquals(DeleteProfileState.SHOWDIALOG, viewModel.state.value.deleteProfileState)
            coVerify(exactly = 0) { messagesRepo.saveMessage(any(), any(), any()) }
            coVerify(exactly = 0) { messagesRepo.syncMessages() }
        }

    @Test
    fun `GIVEN no selected user WHEN delete is requested THEN deletes with an empty wallet`() =
        runTest {
            coEvery { userRepo.deleteUser(any()) } returns false
            val viewModel = createViewModel()

            viewModel.handleAction(UserSelectAction.DeleteUser)

            coVerify { userRepo.deleteUser("") }
            assertEquals(DeleteProfileState.INITIAL, viewModel.state.value.deleteProfileState)
        }

    @Test
    fun `WHEN delete profile state is updated THEN each state is reflected`() =
        runTest {
            val viewModel = createViewModel()

            DeleteProfileState.entries.forEach { deleteState ->
                viewModel.handleAction(UserSelectAction.UpdateDeleteProfileState(deleteState))
                assertEquals(deleteState, viewModel.state.value.deleteProfileState)
            }
        }

    // Edit selected user

    @Test
    fun `GIVEN no selected user WHEN updating the selected user THEN nothing changes`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(firstName = "Nobody"))

            assertNull(viewModel.state.value.selectedUser)
        }

    @Test
    fun `WHEN last name and photo are updated THEN other fields are kept`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(lastName = "Newlast", photoPath = "/new/photo.jpg"))

            val selected = viewModel.state.value.selectedUser!!
            assertEquals(planter.firstName, selected.firstName)
            assertEquals("Newlast", selected.lastName)
            assertEquals("/new/photo.jpg", selected.photoPath)
            assertEquals(planter.wallet, selected.wallet)
            assertEquals(planter.id, selected.id)
        }

    @Test
    fun `WHEN phone is updated THEN it becomes the wallet`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(phone = "0799999999"))

            assertEquals(
                "0799999999",
                viewModel.state.value.selectedUser!!
                    .wallet,
            )
        }

    @Test
    fun `WHEN only email is updated THEN it becomes the wallet`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(email = "new@example.com"))

            assertEquals(
                "new@example.com",
                viewModel.state.value.selectedUser!!
                    .wallet,
            )
        }

    @Test
    fun `WHEN both phone and email are updated THEN the phone wins as wallet`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(phone = "0799999999", email = "new@example.com"))

            assertEquals(
                "0799999999",
                viewModel.state.value.selectedUser!!
                    .wallet,
            )
        }

    @Test
    fun `WHEN updating the selected user THEN the user list is untouched`() =
        runTest {
            every { userRepo.users() } returns flowOf(listOf(planter))
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))

            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(firstName = "Changed"))

            assertEquals(listOf(planter), viewModel.state.value.users)
        }

    // Save

    @Test
    fun `GIVEN an edited selected user WHEN saved THEN the edited user is written to the repo`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))
            viewModel.handleAction(UserSelectAction.UpdateSelectedUser(firstName = "Edited"))

            viewModel.handleAction(UserSelectAction.SaveUserToDatabase)

            coVerify(timeout = 5_000) { userRepo.updateUser(planter.copy(firstName = "Edited")) }
        }

    @Test
    fun `GIVEN no selected user WHEN saved THEN nothing is written`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.handleAction(UserSelectAction.SaveUserToDatabase)

            coVerify(exactly = 0) { userRepo.updateUser(any()) }
        }

    // Screen-handled actions

    @Test
    fun `WHEN navigation actions are sent THEN state is unchanged and no events are emitted`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.handleAction(UserSelectAction.SelectUser(planter))
            val before = viewModel.state.value

            viewModel.handleAction(UserSelectAction.NavigateBack)
            viewModel.handleAction(UserSelectAction.NavigateToPhoto)

            assertEquals(before, viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }
}