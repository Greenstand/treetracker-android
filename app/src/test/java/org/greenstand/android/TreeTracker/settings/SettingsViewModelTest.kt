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
package org.greenstand.android.TreeTracker.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN init THEN loads power user`() = runTest {
        val powerUser = FakeFileGenerator.fakeUsers.first()
        coEvery { userRepo.getPowerUser() } returns powerUser

        val viewModel = SettingsViewModel(userRepo)

        val state = viewModel.state.first()
        assertNotNull(state.powerUser)
        assertEquals(powerUser.id, state.powerUser!!.id)
    }

    @Test
    fun `WHEN setPrivacyDialogVisibility true THEN showPrivacyPolicyDialog is true`() = runTest {
        coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

        val viewModel = SettingsViewModel(userRepo)
        viewModel.setPrivacyDialogVisibility(true)

        val state = viewModel.state.first()
        assertTrue(state.showPrivacyPolicyDialog!!)
    }

    @Test
    fun `WHEN setPrivacyDialogVisibility false THEN showPrivacyPolicyDialog is false`() = runTest {
        coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

        val viewModel = SettingsViewModel(userRepo)
        viewModel.setPrivacyDialogVisibility(false)

        val state = viewModel.state.first()
        assertFalse(state.showPrivacyPolicyDialog!!)
    }

    @Test
    fun `WHEN logout THEN calls setPowerUserStatus and hides logout dialog`() = runTest {
        val powerUser = FakeFileGenerator.fakeUsers.first()
        coEvery { userRepo.getPowerUser() } returns powerUser

        val viewModel = SettingsViewModel(userRepo)

        viewModel.logout()

        coVerify { userRepo.setPowerUserStatus(powerUser.id, false) }
        val state = viewModel.state.first()
        assertFalse(state.showLogoutDialog!!)
    }

    @Test
    fun `WHEN updateLogoutDialogVisibility true THEN shows dialog`() = runTest {
        coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

        val viewModel = SettingsViewModel(userRepo)
        viewModel.updateLogoutDialogVisibility(true)

        val state = viewModel.state.first()
        assertTrue(state.showLogoutDialog!!)
    }

    @Test
    fun `WHEN updateLogoutDialogVisibility false THEN hides dialog`() = runTest {
        coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

        val viewModel = SettingsViewModel(userRepo)
        viewModel.updateLogoutDialogVisibility(false)

        val state = viewModel.state.first()
        assertFalse(state.showLogoutDialog!!)
    }
}
