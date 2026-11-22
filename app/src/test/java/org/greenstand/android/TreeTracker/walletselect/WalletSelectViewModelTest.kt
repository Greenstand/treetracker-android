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
package org.greenstand.android.TreeTracker.walletselect

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
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
class WalletSelectViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    private lateinit var captureSetupData: CaptureSetupData

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.isOpen } returns true
        captureSetupData = mockk(relaxed = true)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `WHEN selectPlanter called THEN finds user and updates selectedUser in state`() =
        runTest {
            val users = FakeFileGenerator.fakeUsers
            val targetUser = users.first()

            every { captureSetupData.user } returns targetUser
            every { userRepo.users() } returns flowOf(users)
            coEvery { userRepo.getUserList() } returns users

            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))

            viewModel.handleAction(WalletSelectAction.SelectPlanter(targetUser.id))

            val state = viewModel.state.first()
            assertNotNull(state.selectedUser)
            assertEquals(targetUser.id, state.selectedUser!!.id)
        }

    private val current = FakeFileGenerator.fakeUsers[0].copy(wallet = "current-wallet")
    private val other = FakeFileGenerator.fakeUsers[1].copy(wallet = "other-wallet")
    private val third = FakeFileGenerator.fakeUsers[2].copy(wallet = "third-wallet")

    @Test
    fun `WHEN created THEN current user is set and excluded from alternate users`() =
        runTest {
            every { captureSetupData.user } returns current
            every { userRepo.users() } returns flowOf(listOf(current, other, third))

            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))

            val state = viewModel.state.value
            assertEquals(current, state.currentUser)
            assertEquals(listOf(other, third), state.alternateUsers)
            assertNull(state.selectedUser)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `GIVEN no current user WHEN created THEN setup is lost and no users are listed`() =
        runTest {
            every { captureSetupData.user } returns null
            every { userRepo.users() } returns flowOf(listOf(current, other))

            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))

            assertTrue(viewModel.state.value.setupLost)
            assertNull(viewModel.state.value.currentUser)
            assertEquals(emptyList(), viewModel.state.value.alternateUsers)
        }

    @Test
    fun `WHEN the users flow emits again THEN alternate users are refreshed`() =
        runTest {
            val users = MutableStateFlow(listOf(current, other))
            every { captureSetupData.user } returns current
            every { userRepo.users() } returns users
            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
            assertEquals(listOf(other), viewModel.state.value.alternateUsers)

            users.value = listOf(current, other, third)

            assertEquals(listOf(other, third), viewModel.state.value.alternateUsers)
            assertEquals(current, viewModel.state.value.currentUser)
        }

    @Test
    fun `WHEN a planter is selected THEN its wallet becomes the destination wallet`() =
        runTest {
            every { captureSetupData.user } returns current
            every { userRepo.users() } returns flowOf(listOf(current, other))
            coEvery { userRepo.getUserList() } returns listOf(current, other)
            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))

            viewModel.handleAction(WalletSelectAction.SelectPlanter(other.id))

            assertEquals(other, viewModel.state.value.selectedUser)
            verify { captureSetupData.destinationWallet = "other-wallet" }
        }

    @Test
    fun `WHEN a different planter is selected THEN the selection and destination wallet change`() =
        runTest {
            every { captureSetupData.user } returns current
            every { userRepo.users() } returns flowOf(listOf(current, other, third))
            coEvery { userRepo.getUserList() } returns listOf(current, other, third)
            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
            viewModel.handleAction(WalletSelectAction.SelectPlanter(other.id))

            viewModel.handleAction(WalletSelectAction.SelectPlanter(third.id))

            assertEquals(third, viewModel.state.value.selectedUser)
            verify { captureSetupData.destinationWallet = "third-wallet" }
        }

    @Test
    fun `WHEN an unknown planter is selected THEN selection and destination wallet are cleared`() =
        runTest {
            every { captureSetupData.user } returns current
            every { userRepo.users() } returns flowOf(listOf(current, other))
            coEvery { userRepo.getUserList() } returns listOf(current, other)
            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
            viewModel.handleAction(WalletSelectAction.SelectPlanter(other.id))

            viewModel.handleAction(WalletSelectAction.SelectPlanter(-99L))

            assertNull(viewModel.state.value.selectedUser)
            verify { captureSetupData.destinationWallet = null }
        }

    @Test
    fun `WHEN navigation actions are sent THEN state is unchanged and no events are emitted`() =
        runTest {
            every { captureSetupData.user } returns current
            every { userRepo.users() } returns flowOf(listOf(current, other))
            val viewModel = WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), mockk(relaxed = true)))
            val before = viewModel.state.value

            viewModel.handleAction(WalletSelectAction.NavigateToUserSelect)
            viewModel.handleAction(WalletSelectAction.NavigateForward)
            viewModel.handleAction(WalletSelectAction.NavigateToAddWallet)
            viewModel.handleAction(WalletSelectAction.NavigateBack)

            assertEquals(before, viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
            verify(exactly = 0) { captureSetupData.destinationWallet = any() }
        }

    // --- Restored after process death ---

    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val restoreUserRepo = mockk<UserRepo>(relaxed = true)

    private fun restoredViewModel(): WalletSelectViewModel {
        every { CaptureSetupScopeManager.isOpen } returns false
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { CaptureSetupScopeManager.nav } returns setupNav
        return WalletSelectViewModel(userRepo, CaptureSetupRestorer(mockk(relaxed = true), restoreUserRepo))
    }

    @Test
    fun `WHEN restored after process death THEN reopens setup with the saved user`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            coEvery { restoreUserRepo.getUser(any()) } returns fakeUser

            val viewModel = restoredViewModel()

            verify { captureSetupData.user = fakeUser }
            assertEquals(fakeUser, viewModel.state.value.currentUser)
            assertFalse(viewModel.state.value.restoring)
            assertFalse(viewModel.state.value.setupLost)
        }

    @Test
    fun `WHEN restored and still reloading the user THEN it is restoring`() =
        runTest {
            val reload = CompletableDeferred<User?>()
            coEvery { restoreUserRepo.getUser(any()) } coAnswers { reload.await() }

            val viewModel = restoredViewModel()
            assertTrue(viewModel.state.value.restoring)

            reload.complete(FakeFileGenerator.fakeUsers.first())
            assertFalse(viewModel.state.value.restoring)
        }

    @Test
    fun `WHEN restored and the saved user is gone THEN setup is lost and input does not touch the closed scope`() =
        runTest {
            coEvery { restoreUserRepo.getUser(any()) } returns null

            val viewModel = restoredViewModel()
            viewModel.handleAction(WalletSelectAction.SelectPlanter(other.id))

            assertTrue(viewModel.state.value.setupLost)
            assertFalse(viewModel.state.value.restoring)
            verify { CaptureSetupScopeManager.close() }
            verify(exactly = 0) { captureSetupData.destinationWallet = any() }
            assertNull(viewModel.state.value.selectedUser)
        }
}