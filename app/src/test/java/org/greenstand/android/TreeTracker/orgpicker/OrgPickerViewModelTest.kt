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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.OrgRoute
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class OrgPickerViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private lateinit var orgPickerViewModel: OrgPickerViewModel

    @Before
    fun setup() {
        orgPickerViewModel = OrgPickerViewModel(orgRepo)
    }

    @Test
    @Throws(Exception::class)
    fun `verify Org Repo gets correct Org Set`() =
        runTest {
            val orgList = FakeFileGenerator.fakeOrganizationList
            coEvery { orgRepo.getOrgs() } returns orgList

            coVerify { orgRepo.getOrgs() }
        }

    @Test
    @Throws(Exception::class)
    fun `set fake organization, returns success with valid Org`() =
        runTest {
            val currentOrg = FakeFileGenerator.fakeOrganizationList.first()
            // Given
            coEvery { orgRepo.currentOrg() } returns currentOrg

            // When
            orgPickerViewModel.handleAction(OrgPickerAction.SetOrg(FakeFileGenerator.fakeOrganizationList.first()))

            // Assert state has correct data
            val result = orgPickerViewModel.state.value.currentOrg
            Assert.assertEquals(result, FakeFileGenerator.fakeOrganizationList.first())
        }

    @Test
    fun `WHEN NavigateNext action THEN emits PopBackStackEvent`() =
        runTest {
            orgPickerViewModel.handleAction(OrgPickerAction.NavigateNext)

            val event = orgPickerViewModel.events.first().getContentIfNotConsumed()
            Assert.assertEquals(PopBackStackEvent, event)
        }

    private val orgs = FakeFileGenerator.fakeOrganizationList

    @Test
    fun `WHEN created THEN state holds all orgs and the current org`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns orgs
            every { orgRepo.currentOrg() } returns orgs[1]

            val viewModel = OrgPickerViewModel(orgRepo)

            Assert.assertEquals(OrgPickerState(orgs = orgs, currentOrg = orgs[1]), viewModel.state.value)
            Assert.assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN created with no orgs THEN state has an empty org list`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns emptyList()
            every { orgRepo.currentOrg() } returns orgs[0]

            val viewModel = OrgPickerViewModel(orgRepo)

            Assert.assertTrue(
                viewModel.state.value.orgs
                    .isEmpty(),
            )
            Assert.assertEquals(orgs[0], viewModel.state.value.currentOrg)
        }

    @Test
    fun `WHEN an org is set THEN repo is updated before reading back the current org`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns orgs
            every { orgRepo.currentOrg() } returns orgs[0]
            val viewModel = OrgPickerViewModel(orgRepo)

            viewModel.handleAction(OrgPickerAction.SetOrg(orgs[1]))

            coVerifyOrder {
                orgRepo.setOrg("newId")
                orgRepo.currentOrg()
            }
        }

    @Test
    fun `WHEN the repo falls back to another org after set THEN state shows the repo's current org`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns orgs
            every { orgRepo.currentOrg() } returnsMany listOf(orgs[1], orgs[0])
            val viewModel = OrgPickerViewModel(orgRepo)
            Assert.assertEquals(orgs[1], viewModel.state.value.currentOrg)

            viewModel.handleAction(OrgPickerAction.SetOrg(orgs[1]))

            Assert.assertEquals(orgs[0], viewModel.state.value.currentOrg)
        }

    @Test
    fun `WHEN an org is set THEN org list is unchanged and no events are emitted`() =
        runTest {
            coEvery { orgRepo.getOrgs() } returns orgs
            every { orgRepo.currentOrg() } returns orgs[1]
            val viewModel = OrgPickerViewModel(orgRepo)

            viewModel.handleAction(OrgPickerAction.SetOrg(orgs[1]))

            Assert.assertEquals(orgs, viewModel.state.value.orgs)
            Assert.assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN NavigateNext pop is dispatched THEN navigator returns to the previous screen`() =
        runTest {
            val navigator = fakeNavigator(DashboardRoute, OrgRoute)

            orgPickerViewModel.handleAction(OrgPickerAction.NavigateNext)
            val event = orgPickerViewModel.events.first().getContentIfNotConsumed()
            Assert.assertEquals(PopBackStackEvent, event)
            navigator.throttledPopBackStack()

            Assert.assertEquals(listOf(DashboardRoute), navigator.backStack)
        }

    @Test
    fun `WHEN NavigateNext is sent twice THEN two pop events are emitted`() =
        runTest {
            orgPickerViewModel.handleAction(OrgPickerAction.NavigateNext)
            orgPickerViewModel.handleAction(OrgPickerAction.NavigateNext)

            val events = orgPickerViewModel.events.replayCache.mapNotNull { it.getContentIfNotConsumed() }
            Assert.assertEquals(listOf(PopBackStackEvent, PopBackStackEvent), events)
        }
}