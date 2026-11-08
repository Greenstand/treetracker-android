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
package org.greenstand.android.TreeTracker.models.organization

import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrgFeatureTest {
    private val orgRepo: OrgRepo = mockk()
    private val resolver = FeatureResolver(orgRepo)

    private fun givenOrg(
        captureSetupFlow: List<Destination> = emptyList(),
        captureFlow: List<Destination> = emptyList(),
    ) {
        every { orgRepo.currentOrg() } returns
            Org(
                id = "42",
                name = "Acme",
                walletId = "wallet",
                logoPath = "",
                captureSetupFlow = captureSetupFlow,
                captureFlow = captureFlow,
            )
    }

    @Test
    fun `fromKey maps every feature key to its feature`() {
        OrgFeature.entries.forEach { feature ->
            assertEquals(feature, OrgFeature.fromKey(feature.key))
        }
    }

    @Test
    fun `force note uses the forceNote json key`() {
        assertEquals("forceNote", OrgFeature.FORCE_NOTE.key)
    }

    @Test
    fun `fromKey returns null for an unknown key`() {
        assertNull(OrgFeature.fromKey("teleport"))
    }

    @Test
    fun `fromKey returns null for an empty key`() {
        assertNull(OrgFeature.fromKey(""))
    }

    @Test
    fun `fromKey is case sensitive`() {
        assertNull(OrgFeature.fromKey("FORCENOTE"))
        assertNull(OrgFeature.fromKey("forcenote"))
    }

    @Test
    fun `fromKey does not match the enum name`() {
        assertNull(OrgFeature.fromKey(OrgFeature.FORCE_NOTE.name))
    }

    @Test
    fun `capture flow feature is enabled when the route lists it`() {
        givenOrg(
            captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                ),
        )

        assertTrue(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow feature is enabled for every feature a route lists`() {
        OrgFeature.entries.forEach { feature ->
            givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(feature.key))))

            assertTrue(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, feature))
        }
    }

    @Test
    fun `capture flow feature is disabled when the route has no features`() {
        givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW)))

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow feature is disabled when the route has an empty feature list`() {
        givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, emptyList())))

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow feature is disabled when the route only lists unknown features`() {
        givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf("teleport"))))

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `unknown features next to a known one do not hide it`() {
        givenOrg(
            captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf("teleport", OrgFeature.FORCE_NOTE.key)),
                ),
        )

        assertTrue(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow feature is disabled when the route is not in the flow`() {
        givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_CAPTURE, listOf(OrgFeature.FORCE_NOTE.key))))

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow feature only applies to the route that lists it`() {
        givenOrg(
            captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                ),
        )

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_CAPTURE, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow feature is disabled for an empty flow`() {
        givenOrg()

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow check ignores features set on the setup flow`() {
        givenOrg(
            captureSetupFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key))),
            captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW)),
        )

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `capture flow uses the first destination when a route appears twice`() {
        givenOrg(
            captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
                ),
        )

        assertTrue(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `setup flow feature is enabled when the route lists it`() {
        givenOrg(
            captureSetupFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                    Destination(RouteRegistry.ROUTE_SESSION_NOTE, listOf(OrgFeature.FORCE_NOTE.key)),
                ),
        )

        assertTrue(resolver.isSetupFlowFeatureEnabled(RouteRegistry.ROUTE_SESSION_NOTE, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `setup flow feature is disabled when the route has no features`() {
        givenOrg(captureSetupFlow = listOf(Destination(RouteRegistry.ROUTE_SESSION_NOTE)))

        assertFalse(resolver.isSetupFlowFeatureEnabled(RouteRegistry.ROUTE_SESSION_NOTE, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `setup flow feature is disabled when the route only lists unknown features`() {
        givenOrg(captureSetupFlow = listOf(Destination(RouteRegistry.ROUTE_SESSION_NOTE, listOf("teleport"))))

        assertFalse(resolver.isSetupFlowFeatureEnabled(RouteRegistry.ROUTE_SESSION_NOTE, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `setup flow feature is disabled when the route is not in the flow`() {
        givenOrg(captureSetupFlow = listOf(Destination(RouteRegistry.ROUTE_USER_SELECT, listOf(OrgFeature.FORCE_NOTE.key))))

        assertFalse(resolver.isSetupFlowFeatureEnabled(RouteRegistry.ROUTE_SESSION_NOTE, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `setup flow check ignores features set on the capture flow`() {
        givenOrg(
            captureSetupFlow = listOf(Destination(RouteRegistry.ROUTE_SESSION_NOTE)),
            captureFlow = listOf(Destination(RouteRegistry.ROUTE_SESSION_NOTE, listOf(OrgFeature.FORCE_NOTE.key))),
        )

        assertFalse(resolver.isSetupFlowFeatureEnabled(RouteRegistry.ROUTE_SESSION_NOTE, OrgFeature.FORCE_NOTE))
    }

    @Test
    fun `resolver reads the current org on every call so an org switch takes effect`() {
        givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key))))
        assertTrue(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))

        givenOrg(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW)))

        assertFalse(resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE))
    }

    @Test(expected = IllegalStateException::class)
    fun `resolver throws when the org repo is not initialized`() {
        every { orgRepo.currentOrg() } throws IllegalStateException("not initialized")

        resolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE)
    }
}