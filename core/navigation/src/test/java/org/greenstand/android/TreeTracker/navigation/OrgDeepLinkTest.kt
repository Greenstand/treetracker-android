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

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OrgDeepLinkTest {
    private fun viewIntent(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

    @Test
    fun `null intent yields empty splash route`() {
        assertEquals(SplashRoute(), parseStartRoute(null))
    }

    @Test
    fun `intent without data yields empty splash route`() {
        assertEquals(SplashRoute(), parseStartRoute(Intent(Intent.ACTION_VIEW)))
    }

    @Test
    fun `valid org link maps id and name`() {
        val route = parseStartRoute(viewIntent("app://mobile.treetracker.org/org?id=abc&name=Acme"))

        assertEquals(SplashRoute(orgId = "abc", orgName = "Acme"), route)
    }

    @Test
    fun `org link with only id leaves name null`() {
        val route = parseStartRoute(viewIntent("app://mobile.treetracker.org/org?id=abc"))

        assertEquals("abc", route.orgId)
        assertNull(route.orgName)
    }

    @Test
    fun `org link without query params yields null args`() {
        val route = parseStartRoute(viewIntent("app://mobile.treetracker.org/org"))

        assertNull(route.orgId)
        assertNull(route.orgName)
    }

    @Test
    fun `name with url-encoded spaces is decoded`() {
        val route = parseStartRoute(viewIntent("app://mobile.treetracker.org/org?id=abc&name=Green%20Trees"))

        assertEquals("Green Trees", route.orgName)
    }

    @Test
    fun `wrong host is ignored`() {
        val route = parseStartRoute(viewIntent("app://evil.example.com/org?id=abc&name=Acme"))

        assertEquals(SplashRoute(), route)
    }

    @Test
    fun `wrong scheme is ignored`() {
        val route = parseStartRoute(viewIntent("https://mobile.treetracker.org/org?id=abc&name=Acme"))

        assertEquals(SplashRoute(), route)
    }

    @Test
    fun `wrong path is ignored`() {
        val route = parseStartRoute(viewIntent("app://mobile.treetracker.org/other?id=abc&name=Acme"))

        assertEquals(SplashRoute(), route)
    }

    @Test
    fun `path prefixed with org still matches`() {
        val route = parseStartRoute(viewIntent("app://mobile.treetracker.org/organization?id=abc"))

        assertEquals("abc", route.orgId)
    }
}