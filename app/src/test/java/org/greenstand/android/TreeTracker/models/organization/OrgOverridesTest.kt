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

import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OrgOverridesTest {
    private val forceNoteKey = featureKey(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE)

    private val org =
        Org(
            id = "42",
            name = "Test Org",
            walletId = "org-wallet",
            logoPath = "",
            captureSetupFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_USER_SELECT),
                    Destination(RouteRegistry.ROUTE_ADD_ORG),
                ),
            captureFlow =
                listOf(
                    Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                    Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW),
                ),
        )

    private fun routes(flow: List<Destination>) = flow.map { it.route }

    @Test
    fun `switched off overrides leave the org unchanged`() {
        val overrides =
            OrgOverrides(
                enabled = false,
                setupSteps = mapOf(RouteRegistry.ROUTE_ADD_ORG to false),
                walletId = "other",
            )

        assertSame(org, overrides.applyTo(org))
    }

    @Test
    fun `no overrides leave the flows as the org set them`() {
        assertEquals(org, OrgOverrides(enabled = true).applyTo(org))
    }

    @Test
    fun `a step turned off is removed from the flow`() {
        val result = OrgOverrides(enabled = true, setupSteps = mapOf(RouteRegistry.ROUTE_ADD_ORG to false)).applyTo(org)

        assertEquals(listOf(RouteRegistry.ROUTE_USER_SELECT), routes(result.captureSetupFlow))
    }

    @Test
    fun `steps turned on are inserted in their canonical place`() {
        val overrides =
            OrgOverrides(
                enabled = true,
                setupSteps =
                    mapOf(
                        RouteRegistry.ROUTE_SESSION_NOTE to true,
                        RouteRegistry.ROUTE_WALLET_SELECT to true,
                    ),
                captureSteps = mapOf(RouteRegistry.ROUTE_TREE_HEIGHT to true),
            )

        val result = overrides.applyTo(org)

        assertEquals(
            listOf(
                RouteRegistry.ROUTE_USER_SELECT,
                RouteRegistry.ROUTE_WALLET_SELECT,
                RouteRegistry.ROUTE_ADD_ORG,
                RouteRegistry.ROUTE_SESSION_NOTE,
            ),
            routes(result.captureSetupFlow),
        )
        assertEquals(
            listOf(
                RouteRegistry.ROUTE_TREE_CAPTURE,
                RouteRegistry.ROUTE_TREE_IMAGE_REVIEW,
                RouteRegistry.ROUTE_TREE_HEIGHT,
            ),
            routes(result.captureFlow),
        )
    }

    @Test
    fun `a step the org already has is not added twice, even under an alias`() {
        val aliased = org.copy(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_CAPTURE), Destination("image-review")))

        val result =
            OrgOverrides(enabled = true, captureSteps = mapOf(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW to true)).applyTo(aliased)

        assertEquals(listOf(RouteRegistry.ROUTE_TREE_CAPTURE, "image-review"), routes(result.captureFlow))
    }

    @Test
    fun `turning off an aliased step removes it`() {
        val aliased = org.copy(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_CAPTURE), Destination("image-review")))

        val result =
            OrgOverrides(enabled = true, captureSteps = mapOf(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW to false)).applyTo(aliased)

        assertEquals(listOf(RouteRegistry.ROUTE_TREE_CAPTURE), routes(result.captureFlow))
    }

    @Test
    fun `required steps cannot be turned off`() {
        val overrides =
            OrgOverrides(
                enabled = true,
                setupSteps = mapOf(RouteRegistry.ROUTE_USER_SELECT to false),
                captureSteps = mapOf(RouteRegistry.ROUTE_TREE_CAPTURE to false),
            )

        assertEquals(org, overrides.applyTo(org))
    }

    @Test
    fun `an org flow in a custom order keeps that order`() {
        val custom =
            org.copy(
                captureSetupFlow =
                    listOf(
                        Destination(RouteRegistry.ROUTE_USER_SELECT),
                        Destination(RouteRegistry.ROUTE_SESSION_NOTE),
                        Destination(RouteRegistry.ROUTE_ADD_ORG),
                    ),
            )

        val result =
            OrgOverrides(enabled = true, setupSteps = mapOf(RouteRegistry.ROUTE_WALLET_SELECT to true)).applyTo(custom)

        assertEquals(
            listOf(
                RouteRegistry.ROUTE_USER_SELECT,
                RouteRegistry.ROUTE_WALLET_SELECT,
                RouteRegistry.ROUTE_SESSION_NOTE,
                RouteRegistry.ROUTE_ADD_ORG,
            ),
            routes(result.captureSetupFlow),
        )
    }

    @Test
    fun `a feature turned on is added to its step`() {
        val result = OrgOverrides(enabled = true, features = mapOf(forceNoteKey to true)).applyTo(org)

        assertEquals(listOf(OrgFeature.FORCE_NOTE.key), result.captureFlow[1].features)
        assertEquals(null, result.captureFlow[0].features)
    }

    @Test
    fun `a feature turned off is removed from its step and other features stay`() {
        val withNote =
            org.copy(
                captureFlow =
                    listOf(
                        Destination(RouteRegistry.ROUTE_TREE_CAPTURE),
                        Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key, "somethingElse")),
                    ),
            )

        val result = OrgOverrides(enabled = true, features = mapOf(forceNoteKey to false)).applyTo(withNote)

        assertEquals(listOf("somethingElse"), result.captureFlow[1].features)
    }

    @Test
    fun `a feature on a step that is added by an override is applied too`() {
        val noReview = org.copy(captureFlow = listOf(Destination(RouteRegistry.ROUTE_TREE_CAPTURE)))
        val overrides =
            OrgOverrides(
                enabled = true,
                captureSteps = mapOf(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW to true),
                features = mapOf(forceNoteKey to true),
            )

        val result = overrides.applyTo(noReview)

        assertEquals(
            Destination(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, listOf(OrgFeature.FORCE_NOTE.key)),
            result.captureFlow[1],
        )
    }

    @Test
    fun `the wallet override replaces the org wallet`() {
        assertEquals("other", OrgOverrides(enabled = true, walletId = "other").applyTo(org).walletId)
        assertEquals("org-wallet", OrgOverrides(enabled = true).applyTo(org).walletId)
    }

    @Test
    fun `withStep and withFeature set and clear single overrides`() {
        val set =
            OrgOverrides()
                .withStep(OrgFlow.SETUP, RouteRegistry.ROUTE_SESSION_NOTE, true)
                .withStep(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_HEIGHT, false)
                .withFeature(forceNoteKey, true)

        assertEquals(mapOf(RouteRegistry.ROUTE_SESSION_NOTE to true), set.setupSteps)
        assertEquals(mapOf(RouteRegistry.ROUTE_TREE_HEIGHT to false), set.captureSteps)
        assertEquals(mapOf(forceNoteKey to true), set.features)

        val cleared =
            set
                .withStep(OrgFlow.SETUP, RouteRegistry.ROUTE_SESSION_NOTE, null)
                .withStep(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_HEIGHT, null)
                .withFeature(forceNoteKey, null)

        assertEquals(OrgOverrides(), cleared)
    }
}