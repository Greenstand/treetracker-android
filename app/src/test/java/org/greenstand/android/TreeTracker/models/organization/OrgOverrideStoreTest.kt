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

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers keeping org link overrides in preferences (so they survive a restart) and
 * [OrgRepo.currentOrg] applying them on top of the org.
 */
@RunWith(RobolectricTestRunner::class)
class OrgOverrideStoreTest {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var prefs: Preferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPrefs = context.getSharedPreferences("org-override-store-test", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        prefs = Preferences(sharedPrefs)
    }

    private fun createStore(isDebugBuild: Boolean = true) = OrgOverrideStore(prefs, orgTestJson, isDebugBuild)

    private val sessionNoteOn =
        OrgOverrides(enabled = true, setupSteps = mapOf(RouteRegistry.ROUTE_SESSION_NOTE to true), walletId = "w")

    @Test
    fun `a new store has no overrides`() {
        val store = createStore()

        assertEquals(OrgOverrides(), store.overrides.value)
        assertNull(store.active)
    }

    @Test
    fun `overrides survive a restart`() {
        createStore().update { sessionNoteOn }

        val restarted = createStore()

        assertEquals(sessionNoteOn, restarted.overrides.value)
        assertEquals(sessionNoteOn, restarted.active)
    }

    @Test
    fun `stored overrides that are switched off are kept but not active`() {
        createStore().update { sessionNoteOn.copy(enabled = false) }

        val restarted = createStore()

        assertEquals(sessionNoteOn.copy(enabled = false), restarted.overrides.value)
        assertNull(restarted.active)
    }

    @Test
    fun `clear removes the stored overrides`() {
        val store = createStore()
        store.update { sessionNoteOn }

        store.clear()

        assertEquals(OrgOverrides(), store.overrides.value)
        assertEquals(OrgOverrides(), createStore().overrides.value)
    }

    @Test
    fun `release builds ignore stored overrides`() {
        createStore().update { sessionNoteOn }

        val release = createStore(isDebugBuild = false)

        assertEquals(OrgOverrides(), release.overrides.value)
        assertNull(release.active)
    }

    @Test
    fun `unreadable stored overrides are ignored`() {
        prefs.edit().putString(OrgOverrideStore.KEY, "{not json").commit()

        assertEquals(OrgOverrides(), createStore().overrides.value)
    }

    @Test
    fun `OrgRepo currentOrg applies active overrides and baseCurrentOrg does not`() =
        runTest {
            val orgStore = FakeOrgStore()
            val overrideStore = createStore()
            val repo = OrgRepo(dao = orgStore.dao, prefs = prefs, json = orgTestJson, overrideStore = overrideStore)
            repo.init()
            val base = repo.baseCurrentOrg()

            assertEquals(base, repo.currentOrg())

            overrideStore.update { sessionNoteOn }

            assertEquals(base, repo.baseCurrentOrg())
            assertEquals("w", repo.currentOrg().walletId)
            assertEquals(
                RouteRegistry.ROUTE_SESSION_NOTE,
                repo
                    .currentOrg()
                    .captureSetupFlow
                    .last()
                    .route,
            )

            overrideStore.update { it.copy(enabled = false) }

            assertEquals(base, repo.currentOrg())
        }
}