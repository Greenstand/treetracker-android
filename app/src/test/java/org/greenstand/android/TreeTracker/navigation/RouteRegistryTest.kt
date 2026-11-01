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
package org.greenstand.android.TreeTracker.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRegistryTest {
    @Test
    fun `resolves no-arg routes to NavKey objects`() {
        assertEquals(UserSelectRoute, RouteRegistry.resolveNoArgRoute("user-select"))
        assertEquals(WalletSelectRoute, RouteRegistry.resolveNoArgRoute("wallet-select"))
        assertEquals(AddOrgRoute, RouteRegistry.resolveNoArgRoute("add-org"))
        assertEquals(SessionNoteRoute, RouteRegistry.resolveNoArgRoute("session-note"))
        assertEquals(TreeHeightScreenRoute, RouteRegistry.resolveNoArgRoute("tree-height-selection"))
    }

    @Test
    fun `resolved routes are NavKeys`() {
        val route: NavKey? = RouteRegistry.resolveNoArgRoute("user-select")
        assertTrue(route is NavKey)
    }

    @Test
    fun `arg-requiring routes do not resolve to no-arg route`() {
        assertNull(RouteRegistry.resolveNoArgRoute("capture/{profilePicUrl}"))
        assertNull(RouteRegistry.resolveNoArgRoute("tree-image-review/{photoPath}"))
    }

    @Test
    fun `unknown route resolves to null`() {
        assertNull(RouteRegistry.resolveNoArgRoute("does-not-exist"))
    }

    @Test
    fun `aliases normalize to canonical route strings`() {
        assertEquals("capture/{profilePicUrl}", RouteRegistry.normalize("tree-capture"))
        assertEquals("tree-image-review/{photoPath}", RouteRegistry.normalize("image-review"))
    }

    @Test
    fun `normalize passes through non-alias strings unchanged`() {
        assertEquals("user-select", RouteRegistry.normalize("user-select"))
        assertEquals("unknown", RouteRegistry.normalize("unknown"))
    }

    @Test
    fun `isValidRoute recognizes no-arg, arg, and alias routes`() {
        assertTrue(RouteRegistry.isValidRoute("user-select"))
        assertTrue(RouteRegistry.isValidRoute("capture/{profilePicUrl}"))
        assertTrue(RouteRegistry.isValidRoute("tree-capture"))
    }

    @Test
    fun `isValidRoute rejects unknown routes`() {
        assertFalse(RouteRegistry.isValidRoute("nope"))
    }

    @Test
    fun `route constants keep their org config values`() {
        assertEquals("user-select", RouteRegistry.ROUTE_USER_SELECT)
        assertEquals("wallet-select", RouteRegistry.ROUTE_WALLET_SELECT)
        assertEquals("add-org", RouteRegistry.ROUTE_ADD_ORG)
        assertEquals("session-note", RouteRegistry.ROUTE_SESSION_NOTE)
        assertEquals("tree-height-selection", RouteRegistry.ROUTE_TREE_HEIGHT)
        assertEquals("capture/{profilePicUrl}", RouteRegistry.ROUTE_TREE_CAPTURE)
        assertEquals("tree-image-review/{photoPath}", RouteRegistry.ROUTE_TREE_IMAGE_REVIEW)
    }

    @Test
    fun `aliases of arg routes do not resolve to no-arg routes`() {
        assertNull(RouteRegistry.resolveNoArgRoute("tree-capture"))
        assertNull(RouteRegistry.resolveNoArgRoute("image-review"))
    }

    @Test
    fun `route matching is exact and case sensitive`() {
        assertNull(RouteRegistry.resolveNoArgRoute("User-Select"))
        assertNull(RouteRegistry.resolveNoArgRoute(" user-select"))
        assertFalse(RouteRegistry.isValidRoute("USER-SELECT"))
        assertFalse(RouteRegistry.isValidRoute(""))
    }

    @Test
    fun `isValidRoute recognizes every no-arg route`() {
        listOf("user-select", "wallet-select", "add-org", "session-note", "tree-height-selection").forEach {
            assertTrue(it, RouteRegistry.isValidRoute(it))
        }
    }

    @Test
    fun `isValidRoute recognizes the image review route and its alias`() {
        assertTrue(RouteRegistry.isValidRoute("tree-image-review/{photoPath}"))
        assertTrue(RouteRegistry.isValidRoute("image-review"))
    }

    @Test
    fun `isValidRoute rejects routes that exist only as typed NavKeys`() {
        assertFalse(RouteRegistry.isValidRoute("dashboard"))
        assertFalse(RouteRegistry.isValidRoute("settings"))
    }

    @Test
    fun `normalize leaves canonical arg routes unchanged`() {
        assertEquals("capture/{profilePicUrl}", RouteRegistry.normalize("capture/{profilePicUrl}"))
        assertEquals("tree-image-review/{photoPath}", RouteRegistry.normalize("tree-image-review/{photoPath}"))
    }
}