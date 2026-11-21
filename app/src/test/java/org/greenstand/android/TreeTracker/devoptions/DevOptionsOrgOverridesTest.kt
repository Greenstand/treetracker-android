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
package org.greenstand.android.TreeTracker.devoptions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgFeature
import org.greenstand.android.TreeTracker.models.organization.OrgFlow
import org.greenstand.android.TreeTracker.models.organization.OrgOverrideCatalog
import org.greenstand.android.TreeTracker.models.organization.OrgOverrideStore
import org.greenstand.android.TreeTracker.models.organization.OrgOverrides
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DevOptionsOrgOverridesTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val org =
        Org(
            id = "42",
            name = "Kasiki Hai",
            walletId = "org-wallet",
            logoPath = "",
            captureSetupFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                    Destination(RouteRegistry.ROUTE_SESSION_NOTE),
                ),
            captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                ),
        )

    private val orgRepo: OrgRepo = mockk { every { baseCurrentOrg() } returns org }

    // Real store over relaxed preferences: reads nothing back, writes go nowhere.
    private val store = OrgOverrideStore(mockk(relaxed = true), Json, isDebugBuild = true)

    private val forceNoteKey = OrgOverrideCatalog.features.single().key

    private fun createViewModel() =
        DevOptionsViewModel(
            mockk(relaxed = true),
            mockk<ConvergenceConfiguration>(relaxed = true),
            orgRepo,
            store,
        )

    private val DevOptionsViewModel.orgLink: OrgLinkOverridesState
        get() = assertNotNull(state.value.orgLink)

    private fun List<OrgSettingRow>.row(id: String) = single { it.id == id }

    @Test
    fun `shows what the org sets for every step and feature`() {
        val orgLink = createViewModel().orgLink

        assertEquals("42", orgLink.orgId)
        assertEquals("Kasiki Hai", orgLink.orgName)
        assertFalse(orgLink.enabled)
        assertEquals(
            OrgOverrideCatalog.setupSteps.map { it.route },
            orgLink.setupSteps.map { it.id },
        )
        assertEquals(
            listOf(true, false, false, true),
            orgLink.setupSteps.map { it.orgDefault },
        )
        assertEquals(listOf(true, true, false), orgLink.captureSteps.map { it.orgDefault })
        assertTrue(orgLink.features.row(forceNoteKey).orgDefault)
        assertTrue(orgLink.setupSteps.row(RouteRegistry.ROUTE_USER_SELECT).required)
        assertTrue(orgLink.captureSteps.row(RouteRegistry.ROUTE_TREE_CAPTURE).required)
        assertEquals("org-wallet", orgLink.orgWalletId)
        assertNull(orgLink.walletIdOverride)
        assertFalse(orgLink.hasOverrides)
        assertEquals(listOf("User select", "Session note"), orgLink.effectiveSetupFlow)
        assertEquals(listOf("Tree capture", "Image review (forceNote)"), orgLink.effectiveCaptureFlow)
    }

    @Test
    fun `hides the section when there is no current org`() {
        val viewModel =
            DevOptionsViewModel(
                mockk(relaxed = true),
                mockk<ConvergenceConfiguration>(relaxed = true),
                orgRepoWithoutOrg(),
                store,
            )

        assertNull(viewModel.state.value.orgLink)
    }

    @Test
    fun `overriding a step saves it and shows the new flow once overrides are on`() {
        val viewModel = createViewModel()

        viewModel.handleAction(DevOptionsAction.SetStepOverride(OrgFlow.SETUP, RouteRegistry.ROUTE_SESSION_NOTE, false))
        viewModel.handleAction(DevOptionsAction.SetStepOverride(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_HEIGHT, true))

        val row = viewModel.orgLink.setupSteps.row(RouteRegistry.ROUTE_SESSION_NOTE)
        assertTrue(row.orgDefault)
        assertFalse(row.value)
        assertTrue(row.isOverridden)
        assertTrue(viewModel.orgLink.hasOverrides)
        // Still off: the flows in use are the org's.
        assertEquals(listOf("User select", "Session note"), viewModel.orgLink.effectiveSetupFlow)

        viewModel.handleAction(DevOptionsAction.SetOrgOverridesEnabled(true))

        assertTrue(store.overrides.value.enabled)
        assertEquals(listOf("User select"), viewModel.orgLink.effectiveSetupFlow)
        assertEquals(
            listOf("Tree capture", "Image review (forceNote)", "Tree height"),
            viewModel.orgLink.effectiveCaptureFlow,
        )
    }

    @Test
    fun `resetting one override makes the step follow the org again`() {
        val viewModel = createViewModel()
        viewModel.handleAction(DevOptionsAction.SetFeatureOverride(forceNoteKey, false))
        assertFalse(viewModel.orgLink.features.row(forceNoteKey).value)

        viewModel.handleAction(DevOptionsAction.SetFeatureOverride(forceNoteKey, null))

        val row = viewModel.orgLink.features.row(forceNoteKey)
        assertTrue(row.value)
        assertFalse(row.isOverridden)
        assertEquals(emptyMap(), store.overrides.value.features)
    }

    @Test
    fun `wallet override is saved and a blank one clears it`() {
        val viewModel = createViewModel()

        viewModel.handleAction(DevOptionsAction.SetWalletIdOverride("test-wallet"))
        assertEquals("test-wallet", viewModel.orgLink.walletIdOverride)
        assertEquals("test-wallet", store.overrides.value.walletId)

        viewModel.handleAction(DevOptionsAction.SetWalletIdOverride(" "))
        assertNull(viewModel.orgLink.walletIdOverride)
        assertNull(store.overrides.value.walletId)
    }

    @Test
    fun `reset all clears every override and the master switch`() {
        val viewModel = createViewModel()
        viewModel.handleAction(DevOptionsAction.SetOrgOverridesEnabled(true))
        viewModel.handleAction(DevOptionsAction.SetStepOverride(OrgFlow.SETUP, RouteRegistry.ROUTE_ADD_ORG, true))
        viewModel.handleAction(DevOptionsAction.SetWalletIdOverride("w"))

        viewModel.handleAction(DevOptionsAction.ResetOrgOverrides)

        assertEquals(OrgOverrides(), store.overrides.value)
        assertFalse(viewModel.orgLink.enabled)
        assertFalse(viewModel.orgLink.hasOverrides)
    }

    @Test
    fun `a new screen shows overrides saved earlier`() {
        createViewModel().handleAction(DevOptionsAction.SetStepOverride(OrgFlow.SETUP, RouteRegistry.ROUTE_WALLET_SELECT, true))

        val row = createViewModel().orgLink.setupSteps.row(RouteRegistry.ROUTE_WALLET_SELECT)

        assertFalse(row.orgDefault)
        assertTrue(row.value)
    }

    @Test
    fun `required steps show the org value whatever is stored`() {
        store.update { it.withStep(OrgFlow.SETUP, RouteRegistry.ROUTE_USER_SELECT, false) }

        val row = createViewModel().orgLink.setupSteps.row(RouteRegistry.ROUTE_USER_SELECT)

        assertTrue(row.value)
        assertFalse(row.isOverridden)
    }
}