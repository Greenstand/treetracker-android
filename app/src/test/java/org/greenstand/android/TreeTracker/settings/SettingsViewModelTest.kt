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
import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DeleteProfileRoute
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.ProfileSelectRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.TreeEditUserSelectRoute
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.NavigationEvent
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun `WHEN init THEN loads power user`() =
        runTest {
            val powerUser = FakeFileGenerator.fakeUsers.first()
            coEvery { userRepo.getPowerUser() } returns powerUser

            val viewModel = SettingsViewModel(userRepo)

            val state = viewModel.state.first()
            assertNotNull(state.powerUser)
            assertEquals(powerUser.id, state.powerUser!!.id)
        }

    @Test
    fun `WHEN setPrivacyDialogVisibility true THEN showPrivacyPolicyDialog is true`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

            val viewModel = SettingsViewModel(userRepo)
            viewModel.handleAction(SettingsAction.SetPrivacyDialogVisibility(true))

            val state = viewModel.state.first()
            assertTrue(state.showPrivacyPolicyDialog!!)
        }

    @Test
    fun `WHEN setPrivacyDialogVisibility false THEN showPrivacyPolicyDialog is false`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

            val viewModel = SettingsViewModel(userRepo)
            viewModel.handleAction(SettingsAction.SetPrivacyDialogVisibility(false))

            val state = viewModel.state.first()
            assertFalse(state.showPrivacyPolicyDialog!!)
        }

    @Test
    fun `WHEN logout THEN calls setPowerUserStatus and hides logout dialog`() =
        runTest {
            val powerUser = FakeFileGenerator.fakeUsers.first()
            coEvery { userRepo.getPowerUser() } returns powerUser

            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.Logout)

            coVerify { userRepo.setPowerUserStatus(powerUser.id, false) }
            val state = viewModel.state.first()
            assertFalse(state.showLogoutDialog!!)
        }

    @Test
    fun `WHEN updateLogoutDialogVisibility true THEN shows dialog`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

            val viewModel = SettingsViewModel(userRepo)
            viewModel.handleAction(SettingsAction.UpdateLogoutDialogVisibility(true))

            val state = viewModel.state.first()
            assertTrue(state.showLogoutDialog!!)
        }

    @Test
    fun `WHEN updateLogoutDialogVisibility false THEN hides dialog`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

            val viewModel = SettingsViewModel(userRepo)
            viewModel.handleAction(SettingsAction.UpdateLogoutDialogVisibility(false))

            val state = viewModel.state.first()
            assertFalse(state.showLogoutDialog!!)
        }

    @Test
    fun `WHEN NavigateToProfile action THEN emits NavigationEvent`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.NavigateToProfile)

            val event = viewModel.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent, "Expected NavigationEvent but got $event")
        }

    @Test
    fun `WHEN NavigateToEditTrees action THEN emits NavigationEvent`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.NavigateToEditTrees)

            val event = viewModel.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent, "Expected NavigationEvent but got $event")
        }

    @Test
    fun `WHEN NavigateToMap action THEN emits NavigationEvent`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.NavigateToMap)

            val event = viewModel.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent, "Expected NavigationEvent but got $event")
        }

    @Test
    fun `WHEN NavigateToDeleteAccount action THEN emits NavigationEvent`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.NavigateToDeleteAccount)

            val event = viewModel.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent, "Expected NavigationEvent but got $event")
        }

    @Test
    fun `WHEN NavigateBack action THEN emits PopBackStackEvent`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.NavigateBack)

            val event = viewModel.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
        }

    @Test
    fun `WHEN LogoutConfirmed action THEN logs out and emits NavigationEvent`() =
        runTest {
            val powerUser = FakeFileGenerator.fakeUsers.first()
            coEvery { userRepo.getPowerUser() } returns powerUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.LogoutConfirmed)

            coVerify { userRepo.setPowerUserStatus(powerUser.id, false) }
            val event = viewModel.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent, "Expected NavigationEvent but got $event")
        }

    @Test
    fun `WHEN NavigateToProfile action THEN navigator lands on ProfileSelectRoute`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)
            val navigator = fakeNavigator(DashboardRoute, SettingsRoute)

            viewModel.handleAction(SettingsAction.NavigateToProfile)

            val event = viewModel.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)

            assertEquals(
                listOf(DashboardRoute, SettingsRoute, ProfileSelectRoute),
                navigator.backStack,
            )
        }

    @Test
    fun `WHEN LogoutConfirmed action THEN navigator clears stack to SignupFlowRoute`() =
        runTest {
            val powerUser = FakeFileGenerator.fakeUsers.first()
            coEvery { userRepo.getPowerUser() } returns powerUser
            val viewModel = SettingsViewModel(userRepo)
            val navigator = fakeNavigator(DashboardRoute, SettingsRoute)

            viewModel.handleAction(SettingsAction.LogoutConfirmed)

            val event = viewModel.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)

            // popUpToRoot() + launchSingleTop leaves SignupFlowRoute as the only entry.
            assertEquals(listOf<NavKey>(SignupFlowRoute), navigator.backStack)
        }

    private suspend fun SettingsViewModel.navigateWith(action: SettingsAction): List<NavKey> {
        val navigator = fakeNavigator(DashboardRoute, SettingsRoute)
        handleAction(action)
        val event = events.first().getContentIfNotConsumed() as NavigationEvent
        event.navigate(navigator)
        return navigator.backStack
    }

    @Test
    fun `WHEN init without a power user THEN powerUser is null and no dialog is shown`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null

            val viewModel = SettingsViewModel(userRepo)

            assertEquals(SettingsState(), viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN NavigateToEditTrees action THEN navigator lands on TreeEditUserSelectRoute`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            val backStack = viewModel.navigateWith(SettingsAction.NavigateToEditTrees)

            assertEquals(listOf(DashboardRoute, SettingsRoute, TreeEditUserSelectRoute), backStack)
        }

    @Test
    fun `WHEN NavigateToMap action THEN navigator lands on MapRoute`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            val backStack = viewModel.navigateWith(SettingsAction.NavigateToMap)

            assertEquals(listOf(DashboardRoute, SettingsRoute, MapRoute), backStack)
        }

    @Test
    fun `WHEN NavigateToDeleteAccount action THEN navigator lands on DeleteProfileRoute`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            val backStack = viewModel.navigateWith(SettingsAction.NavigateToDeleteAccount)

            assertEquals(listOf(DashboardRoute, SettingsRoute, DeleteProfileRoute), backStack)
        }

    @Test
    fun `WHEN NavigateBack action THEN pop returns to the dashboard`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)
            val navigator = fakeNavigator(DashboardRoute, SettingsRoute)

            viewModel.handleAction(SettingsAction.NavigateBack)

            assertEquals(PopBackStackEvent, viewModel.events.first().getContentIfNotConsumed())
            // HandleUIEvents dispatches PopBackStackEvent as a throttled pop.
            navigator.throttledPopBackStack()
            assertEquals(listOf<NavKey>(DashboardRoute), navigator.backStack)
        }

    @Test
    fun `WHEN logout without a power user THEN only hides the logout dialog`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null
            val viewModel = SettingsViewModel(userRepo)
            viewModel.handleAction(SettingsAction.UpdateLogoutDialogVisibility(true))

            viewModel.handleAction(SettingsAction.Logout)

            coVerify(exactly = 0) { userRepo.setPowerUserStatus(any(), any()) }
            assertEquals(false, viewModel.state.value.showLogoutDialog)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN LogoutConfirmed action THEN hides the dialog and emits a single navigation`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = SettingsViewModel(userRepo)
            viewModel.handleAction(SettingsAction.UpdateLogoutDialogVisibility(true))

            viewModel.handleAction(SettingsAction.LogoutConfirmed)

            assertEquals(false, viewModel.state.value.showLogoutDialog)
            assertEquals(1, viewModel.events.replayCache.size)
            coVerify(exactly = 1) { userRepo.setPowerUserStatus(FakeFileGenerator.fakeUsers.first().id, false) }
        }

    @Test
    fun `WHEN LogoutConfirmed from a deep stack THEN only SignupFlowRoute remains`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = SettingsViewModel(userRepo)
            val navigator = fakeNavigator(DashboardRoute, SettingsRoute, MapRoute, SettingsRoute)

            viewModel.handleAction(SettingsAction.LogoutConfirmed)
            val event = viewModel.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)

            assertEquals(listOf<NavKey>(SignupFlowRoute), navigator.backStack)
        }

    @Test
    fun `WHEN dialog actions run THEN no events are emitted`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
            val viewModel = SettingsViewModel(userRepo)

            viewModel.handleAction(SettingsAction.SetPrivacyDialogVisibility(true))
            viewModel.handleAction(SettingsAction.UpdateLogoutDialogVisibility(true))

            assertTrue(viewModel.events.replayCache.isEmpty())
            assertNull(viewModel.state.value.showDeleteAccountDialog)
        }
}