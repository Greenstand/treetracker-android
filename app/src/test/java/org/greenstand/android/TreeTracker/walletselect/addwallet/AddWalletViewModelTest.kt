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
package org.greenstand.android.TreeTracker.walletselect.addwallet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AddWalletViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var captureSetupData: CaptureSetupData

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(CaptureSetupScopeManager)
        captureSetupData = mockk(relaxed = true)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `WHEN init THEN sets userImagePath from CaptureSetupScopeManager`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser

            val viewModel = AddWalletViewModel()

            val state = viewModel.state.value
            assertEquals(fakeUser.photoPath, state.userImagePath)
        }

    @Test
    fun `WHEN updateWalletName called THEN updates state walletName and CaptureSetupScopeManager data`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser

            val viewModel = AddWalletViewModel()

            viewModel.handleAction(AddWalletAction.UpdateWalletName("new-wallet-name"))

            val state = viewModel.state.value
            assertEquals("new-wallet-name", state.walletName)
            verify { captureSetupData.destinationWallet = "new-wallet-name" }
        }

    @Test
    fun `WHEN created THEN wallet name is empty and no events are emitted`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()

            val viewModel = AddWalletViewModel()

            assertEquals("", viewModel.state.value.walletName)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN wallet name is typed repeatedly THEN state and destination wallet hold the latest value`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = AddWalletViewModel()

            viewModel.handleAction(AddWalletAction.UpdateWalletName("w"))
            viewModel.handleAction(AddWalletAction.UpdateWalletName("wallet-2"))

            assertEquals("wallet-2", viewModel.state.value.walletName)
            verify { captureSetupData.destinationWallet = "w" }
            verify { captureSetupData.destinationWallet = "wallet-2" }
        }

    @Test
    fun `WHEN wallet name is cleared THEN destination wallet is set to empty`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = AddWalletViewModel()
            viewModel.handleAction(AddWalletAction.UpdateWalletName("wallet"))

            viewModel.handleAction(AddWalletAction.UpdateWalletName(""))

            assertEquals("", viewModel.state.value.walletName)
            verify { captureSetupData.destinationWallet = "" }
        }

    @Test
    fun `WHEN wallet name is updated THEN user image path is kept`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser
            val viewModel = AddWalletViewModel()

            viewModel.handleAction(AddWalletAction.UpdateWalletName("wallet"))

            assertEquals(fakeUser.photoPath, viewModel.state.value.userImagePath)
        }

    @Test
    fun `WHEN navigation actions are sent THEN state is unchanged and no events are emitted`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = AddWalletViewModel()
            viewModel.handleAction(AddWalletAction.UpdateWalletName("wallet"))
            val before = viewModel.state.value

            viewModel.handleAction(AddWalletAction.NavigateBack)
            viewModel.handleAction(AddWalletAction.NavigateNext)

            assertEquals(before, viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
            verify(exactly = 1) { captureSetupData.destinationWallet = any() }
        }
}